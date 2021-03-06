package magenta.artifact

import magenta.Build
import org.scalatest.{FlatSpec, Matchers}

class S3ArtifactTest extends FlatSpec with Matchers {
  "S3Artifact" should "create an S3Artifact instance from a build and bucket" in {
    val build = Build("testProject", "123")
    val artifact = S3JsonArtifact(build, "myBucket")
    artifact.bucket should be("myBucket")
    artifact.key should be("testProject/123/")
  }
}
