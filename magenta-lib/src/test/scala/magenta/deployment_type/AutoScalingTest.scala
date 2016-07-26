package magenta

import java.io.File
import java.util.UUID

import com.amazonaws.services.s3.AmazonS3
import magenta.artifact.S3Package
import magenta.deployment_type.AutoScaling
import magenta.fixtures._
import magenta.tasks._
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.scalatest.{FlatSpec, Matchers}

class AutoScalingTest extends FlatSpec with Matchers {
  implicit val fakeKeyRing = KeyRing()
  implicit val reporter = DeployReporter.rootReporterFor(UUID.randomUUID(), fixtures.parameters())
  implicit val artifactClient: AmazonS3 = null

  "auto-scaling with ELB package type" should "have a deploy action" in {
    val data: Map[String, JValue] = Map(
      "bucket" -> "asg-bucket"
    )

    val app = Seq(App("app"))

    val p = DeploymentPackage("app", app, data, "asg-elb", S3Package("artifact-bucket", "test/123/app"))

    AutoScaling.perAppActions("deploy")(p)(DeploymentResources(reporter, lookupEmpty, artifactClient), DeployTarget(parameters(), UnnamedStack)) should be (List(
      CheckForStabilization(p, PROD, UnnamedStack),
      CheckGroupSize(p, PROD, UnnamedStack),
      SuspendAlarmNotifications(p, PROD, UnnamedStack),
      TagCurrentInstancesWithTerminationTag(p, PROD, UnnamedStack),
      DoubleSize(p, PROD, UnnamedStack),
      HealthcheckGrace(20000),
      WaitForStabilization(p, PROD, UnnamedStack, 15 * 60 * 1000),
      WarmupGrace(1000),
      WaitForStabilization(p, PROD, UnnamedStack, 15 * 60 * 1000),
      CullInstancesWithTerminationTag(p, PROD, UnnamedStack),
      ResumeAlarmNotifications(p, PROD, UnnamedStack)
    ))
  }

  "seconds to wait" should "be overridable" in {
    val data: Map[String, JValue] = Map(
      "bucket" -> "asg-bucket",
      "secondsToWait" -> 3 * 60,
      "healthcheckGrace" -> 30,
      "warmupGrace" -> 20
    )

    val app = Seq(App("app"))

    val p = DeploymentPackage("app", app, data, "asg-elb", S3Package("artifact-bucket", "test/123/app"))

    AutoScaling.perAppActions("deploy")(p)(DeploymentResources(reporter, lookupEmpty, artifactClient), DeployTarget(parameters(), UnnamedStack)) should be (List(
      CheckForStabilization(p, PROD, UnnamedStack),
      CheckGroupSize(p, PROD, UnnamedStack),
      SuspendAlarmNotifications(p, PROD, UnnamedStack),
      TagCurrentInstancesWithTerminationTag(p, PROD, UnnamedStack),
      DoubleSize(p, PROD, UnnamedStack),
      HealthcheckGrace(30000),
      WaitForStabilization(p, PROD, UnnamedStack, 3 * 60 * 1000),
      WarmupGrace(20000),
      WaitForStabilization(p, PROD, UnnamedStack, 3 * 60 * 1000),
      CullInstancesWithTerminationTag(p, PROD, UnnamedStack),
      ResumeAlarmNotifications(p, PROD, UnnamedStack)
    ))
  }
}
