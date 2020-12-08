package eu.kyngas;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.jackson.ObjectMapperCustomizer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@ApplicationScoped
public class ObjectMapperConfig implements ObjectMapperCustomizer {

  @Override
  public void customize(ObjectMapper mapper) {
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.registerModules(new JavaTimeModule(),
                           new SimpleModule()
                               .addSerializer(JsonObject.class, serializer(JsonObject::getMap))
                               .addDeserializer(JsonObject.class, deserializer(Map.class, JsonObject::new))
                               .addSerializer(JsonArray.class, serializer(JsonArray::getList))
                               .addDeserializer(JsonArray.class, deserializer(List.class, JsonArray::new))
    );
    Json.mapper = mapper;
  }

  private static <T> JsonSerializer<T> serializer(Function<T, Object> mapper) {
    return new JsonSerializer<>() {
      @Override
      public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeObject(mapper.apply(value));
      }
    };
  }

  private static <T, S> JsonDeserializer<T> deserializer(Class<S> fromClass, Function<S, T> mapper) {
    return new JsonDeserializer<>() {
      @Override
      public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return mapper.apply(p.getCodec().readValue(p, fromClass));
      }
    };
  }
}
