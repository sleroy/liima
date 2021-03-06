package ch.puzzle.itc.mobiliar.business.auditview.entity;

import ch.puzzle.itc.mobiliar.business.database.entity.MyRevisionEntity;
import lombok.*;
import org.hibernate.envers.RevisionType;

@Getter
@Builder(builderMethodName = "hiddenBuilder")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class AuditViewEntry {
    public static final String RELATION_CONSUMED_RESOURCE = "Consumed Resource";
    public static final String RELATION_PROVIDED_RESOURCE = "Provided Resource";
    long timestamp;
    String type; // Property, Resource, ...
    String name; // PropertyName, ...
    String username;
    String oldValue;
    String value;
    long revision;
    RevisionType mode;
    String editContextName;
    String relation; // consumed resource, ...
    boolean isObfuscatedValue = false;

    public String getModeAsString() {
        return this.getMode().name();
    }

    public static AuditViewEntryBuilder builder(MyRevisionEntity revisionEntity, RevisionType revisionType) {
                return hiddenBuilder()
                        .username(revisionEntity.getUsername())
                        .timestamp(revisionEntity.getTimestamp())
                        .revision(revisionEntity.getId())
                        .mode(revisionType);
    }
}
