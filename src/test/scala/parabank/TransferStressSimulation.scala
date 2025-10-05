package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class TransferStressSimulation extends Simulation {

  // 1️⃣ Configuración HTTP
  val httpConf = http
    .baseUrl(url)
    .header("Content-Type", "application/json")
    .check(status.is(200)) // Toda transferencia debe ser exitosa

  // 2️⃣ Feeder desde CSV con datos de cuentas y montos
  val feeder = csv("transacciones.csv").circular

  // 3️⃣ Escenario de transferencia bancaria
  val scn = scenario("Transferencias Concurrentes")
    .feed(feeder)
    .exec(
      http("transferencia")
        .post("/transfer")
        .queryParam("fromAccountId", "${fromAccountId}")
        .queryParam("toAccountId", "${toAccountId}")
        .queryParam("amount", "${amount}")
        .check(status.is(200))
    )

  // 4️⃣ Inyección de carga para cumplir criterio de 150 TPS sostenidos
  val injectionProfile = Seq(
    rampUsersPerSec(50) to 150 during (30.seconds), // Calentamiento progresivo
    constantUsersPerSec(150) during (2.minutes)     // Mantener 150 TPS
  )

  // 5️⃣ Assertions para validar estabilidad bajo estrés
  setUp(
    scn.inject(injectionProfile).protocols(httpConf)
  ).assertions(
    global.successfulRequests.percent.gte(99),   // No deben perderse transferencias
    global.responseTime.percentile(95).lte(2000) // latencia aceptable
  )
}
