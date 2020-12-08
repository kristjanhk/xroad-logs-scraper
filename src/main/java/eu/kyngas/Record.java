package eu.kyngas;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDate;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Record {
  @Id
  private Long id;

  private String clientMemberClass;
  private String clientMemberCode;
  private String clientSubsystemCode;
  private String clientXRoadInstance;
  private String messageProtocolVersion;
  private Integer producerDurationProducerView;
  private String representedPartyClass;
  private String representedPartyCode;
  private Integer requestAttachmentCount;
  private LocalDate requestInDate;
  private Long requestInTs;
  private Integer requestMimeSize;
  private Integer requestSoapSize;
  private Integer responseAttachmentCount;
  private Integer responseMimeSize;
  private Integer responseSoapSize;
  private String securityServerType;
  private String serviceCode;
  private String serviceMemberClass;
  private String serviceMemberCode;
  private String serviceSubsystemCode;
  private String serviceVersion;
  private String serviceXRoadInstance;
  private Boolean succeeded;
  private Integer totalDuration;

}
