package magenta

import org.joda.time.DateTime

trait DataLookup {
  def keys: Seq[String]
  def all: Map[String,Seq[Datum]]
  def get(key:String): Seq[Datum] = all.get(key).getOrElse(Nil)
  def datum(key: String, app: App, stage: Stage, stack: Stack): Option[Datum]
}

trait HostLookup {
  def all:Seq[Host]
  def get(pkg: DeploymentPackage, app: App, parameters: DeployParameters, stack: Stack):Seq[Host]
}

trait Lookup {
  def name: String
  def lastUpdated: DateTime
  def hosts: HostLookup
  def stages: Seq[String]
  def data: DataLookup
  def keyRing(stage: Stage, apps: Set[App], stack: Stack): KeyRing
  def getLatestAmi(region: String)(tags: Map[String, String]): Option[String]
}

trait SecretProvider {
  def lookup(service: String, account: String): Option[String]
}

trait MagentaCredentials {
  def data: DataLookup
  def secretProvider: SecretProvider
  def keyRing(stage: Stage, apps: Set[App], stack: Stack): KeyRing = KeyRing(
    apiCredentials = apps.toSeq.flatMap {
      app => {
        val KeyPattern = """credentials:(.*)""".r
        val apiCredentials = data.keys flatMap {
          case key@KeyPattern(service) =>
            data.datum(key, app, stage, stack).flatMap { data =>
              secretProvider.lookup(service, data.value).map { secret =>
                service -> ApiCredentials(service, data.value, secret, data.comment)
              }
            }
          case _ => None
        }
        apiCredentials
      }
    }.distinct.toMap
  )
}

