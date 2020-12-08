package eu.kyngas;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import javax.inject.Inject;
import javax.transaction.TransactionManager;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import static java.util.stream.Collectors.toList;

@Slf4j
@Path("record")
public class RecordResource {
  @Inject
  Vertx vertx;
  @Inject
  RecordRepository recordRepository;
  @Inject
  TransactionManager transactionManager;

  @Data
  static class Constraint {
    private final String column;
    private final String operator;
    private final String value;
  }

  @GET
  @Path("{year}/{month}")
  @Produces(MediaType.TEXT_PLAIN)
  public boolean getSeptData(@PathParam("year") int year,
                             @PathParam("month") int month,
                             @QueryParam("subsystem") String subsystem) throws Exception {
    Constraint constraint = new Constraint("clientSubsystemCode", "=", subsystem);
    LocalDate start = LocalDate.of(year, month, 1);
    for (LocalDate date : start.datesUntil(start.plusMonths(1).plusDays(1)).collect(toList())) {
      log.info("Requesting data for date {}", date);
      String[] body = getDataForDate(date, constraint);

      transactionManager.begin();
      log.info("Processing date {}", date);
      int count = processRecords(date, body);

      log.info("Processed {} for date {}", count, date);
      transactionManager.commit();
    }
    return true;
  }

  private String[] getDataForDate(LocalDate date, Constraint constraint) throws IOException {
    WebClient webClient = WebClient.create(vertx, new WebClientOptions()
        .setTrustAll(true)
        .setTryUseCompression(true)
        .addEnabledSecureTransportProtocol("TLSv1.2"));
    HttpRequest<Buffer> request = webClient.getAbs("https://logs.x-tee.ee/EE/api/daily_logs")
        .addQueryParam("date", date.toString())
        .addQueryParam("constraints", List.of(JsonObject.mapFrom(constraint).encode()).toString());
    HttpResponse<Buffer> response = request.sendAndAwait();
    return new String(uncompress(response.body().getBytes())).split("\n");
  }

  private int processRecords(LocalDate date, String[] body) {
    AtomicInteger counter = new AtomicInteger(0);
    List<Record> records = Arrays.stream(body)
        .parallel()
        .map(line -> !line.startsWith("{") ? "{" + line.split("\\{")[1] : line)
        .map(line -> {
          try {
            return new JsonObject(line).mapTo(Record.class);
          } catch (Exception e) {
            log.error("Error", e);
            return null;
          }
        })
        .filter(Objects::nonNull)
        .peek(record -> {
          int i = counter.incrementAndGet();
          if (i % 1000 == 0) {
            log.info("Processing day {}, counter {}", date, i);
          }
        })
        .collect(toList());
    recordRepository.persist(records);
    recordRepository.flush();
    return records.size();
  }

  public static byte[] uncompress(byte[] data) throws IOException {
    GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data));
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      IOUtils.copy(gzip, baos);
    } finally {
      IOUtils.closeQuietly(gzip);
      IOUtils.closeQuietly(baos);
    }
    return baos.toByteArray();
  }
}
