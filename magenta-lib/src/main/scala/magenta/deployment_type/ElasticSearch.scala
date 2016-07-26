package magenta.deployment_type

import magenta.tasks._
import java.io.File

object ElasticSearch extends DeploymentType with S3AclParams {
  def name = "elasticsearch"
  val documentation =
    """
      |A specialised version of the `autoscaling` deployment type that has a specialise health check process to
      |ensure that the resulting ElasticSearch cluster is green.
    """.stripMargin

  val bucket = Param[String]("bucket", "S3 bucket that the artifact should be uploaded into")
  val secondsToWait = Param("secondsToWait",
    """Number of seconds to wait for the ElasticSearch cluster to become green
      | (also used as the wait time for the instance termination)"""
  ).default(15 * 60)

  def perAppActions = {
    case "deploy" => (pkg) => (resources, target) => {
      implicit val keyRing = resources.assembleKeyring(target, pkg)
      val parameters = target.parameters
      val stack = target.stack
      List(
        CheckGroupSize(pkg, parameters.stage, stack),
        WaitForElasticSearchClusterGreen(pkg, parameters.stage, stack, secondsToWait(pkg) * 1000),
        SuspendAlarmNotifications(pkg, parameters.stage, stack),
        TagCurrentInstancesWithTerminationTag(pkg, parameters.stage, stack),
        DoubleSize(pkg, parameters.stage, stack),
        WaitForElasticSearchClusterGreen(pkg, parameters.stage, stack, secondsToWait(pkg) * 1000),
        CullElasticSearchInstancesWithTerminationTag(pkg, parameters.stage, stack, secondsToWait(pkg) * 1000),
        ResumeAlarmNotifications(pkg, parameters.stage, stack)
      )
    }
    case "uploadArtifacts" => (pkg) => (resources, target) =>
      implicit val keyRing = resources.assembleKeyring(target, pkg)
      implicit val artifactClient = resources.artifactClient
      val prefix: String = S3Upload.prefixGenerator(target.stack, target.parameters.stage, pkg.name)
      List(
        S3Upload(
          bucket(pkg),
          Seq(pkg.s3Package -> prefix),
          publicReadAcl = publicReadAcl(pkg)
        )
      )
  }
}
