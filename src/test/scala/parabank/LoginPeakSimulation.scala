package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import parabank.Data._

class LoginPeakSimulation extends Simulation{

  // 1 Http Conf
  val httpConf = http.baseUrl(url)
    .acceptHeader("application/json")
    //Verificar de forma general para todas las solicitudes
    .check(status.is(200))

  // 2 Scenario Definition
  val scn = scenario("Login_Peak").
    exec(http("login")
      .get(s"/login/$username/$password")
       //Recibir información de la cuenta
      .check(status.is(200))
    ).pause(1) // breve pausa para no bombardear instantáneamente

  // Subimos de 100 a 200 concurrentes en 30s, luego mantenemos 200 durante 2 minutos
  val injectionProfile = Seq(
    rampConcurrentUsers(100) to 200 during (30.seconds),
    constantConcurrentUsers(200) during (2.minutes)
  )

  setUp(
    scn.inject(injectionProfile).protocols(httpConf)
  ).assertions(
    details("login").responseTime.percentile(99).lte(5000), // p99 <= 5000 ms
    global.responseTime.max.lte(5000),                      // máximo <= 5s como backstop
    //global.successfulRequests.percent.gte(98)               // tolerancia ligeramente menor en pico
  )
}
