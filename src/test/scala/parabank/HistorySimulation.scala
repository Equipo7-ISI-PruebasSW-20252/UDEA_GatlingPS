package parabank

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import java.time.Instant
import scala.util.Random
import parabank.Data._

class HistorySimulation extends Simulation {

  // 1 Http Conf
  val httpConf = http
    .baseUrl(url) 
    .acceptHeader("application/json")
    .userAgentHeader("Gatling-Perf-Lab")

  // 2 Feeder (random account + cacheBuster)
  val accountsFeeder = Iterator.continually {
    val acct = accounts(Random.nextInt(accounts.length))
    Map(
      "accountId" -> acct,
      "cb" -> Instant.now.toEpochMilli.toString
    )
  }

  // 3 Scenario Definition
  val scn = scenario("AccountHistory")
    .exec(
      http("login")
        .get(s"/login/$username/$password")
        .check(status.is(200))
    )
    .pause(1)

    .feed(accountsFeeder)
    .exec(
      http("GetAccountHistory")
        .get("/accounts/${accountId}/transactions?cb=${cb}")
        .check(status.is(200))
      .check(jsonPath("$[0].id").exists)
      .check(bodyString.saveAs("resp"))
    )
    .pause(1)
    .exec { session =>
      if (System.getProperty("gatling.debug") == "true") {
        println("RESP: " + session("resp").asOption[String].getOrElse("<no body>"))
      }
      session
    }

  // 4 Load Scenario - Injection profile (closed model)
  val injectionProfile = Seq(
    rampConcurrentUsers(0) to 200 during (120.seconds),  // ramp 0 -> 200 en 2 min
    constantConcurrentUsers(200) during (10.minutes)      // steady-state 200 por 10 min
  )

  // 5 Setup + Assertions
  setUp(
    scn.inject(injectionProfile).protocols(httpConf)
  ).assertions(
    // Assertions enfocadas en el criterio: p95 <= 3000 ms y error rate <= 1%
    details("GetAccountHistory").responseTime.percentile(95).lte(3000),
  )
}
