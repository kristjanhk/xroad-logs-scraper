package eu.kyngas;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecordRepository implements PanacheRepository<Record> {
}
