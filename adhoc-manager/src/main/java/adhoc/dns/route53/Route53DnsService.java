/*
 * Copyright (c) 2022-2025 SpeculativeCoder (https://github.com/SpeculativeCoder)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package adhoc.dns.route53;

import adhoc.dns.DnsService;
import adhoc.dns.route53.properties.Route53DnsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.*;

import java.util.List;
import java.util.Set;

@Service
@Profile({"dns-route53"})
@Slf4j
@RequiredArgsConstructor
public class Route53DnsService implements DnsService {

    private final Route53DnsProperties route53DnsProperties;

    private Route53Client route53Client() {
        return Route53Client.builder()
                .region(Region.AWS_GLOBAL)
                .credentialsProvider(credentialsProvider()).build();
    }

    private AwsCredentialsProvider credentialsProvider() {
        return DefaultCredentialsProvider.builder()
                .profileName(route53DnsProperties.getAwsProfileForRoute53())
                .build();
    }

    @Override
    public void createOrUpdateDnsRecord(String domain, Set<String> ips) {
        log.info("Updating Route 53 DNS entry: domain={} ips={}", domain, ips);

        //  TODO
        if (domain.contains("localhost") || ips.isEmpty()) {
            log.warn("Ignoring attempt to set DNS! domain={} ips={}", domain, ips);
            return;
        }

        try (Route53Client route53Client = route53Client()) {
            String hostedZoneId = getHostedZoneId(route53Client);

            ChangeResourceRecordSetsRequest changeResourceRecordSetsRequest = ChangeResourceRecordSetsRequest.builder()
                    .hostedZoneId(hostedZoneId)
                    .changeBatch(ChangeBatch.builder()
                            .changes(upsert(domain, ips)).build()).build();
            log.debug("changeResourceRecordSetsRequest: {}", changeResourceRecordSetsRequest);

            ChangeResourceRecordSetsResponse changeResourceRecordSetsResponse =
                    route53Client.changeResourceRecordSets(changeResourceRecordSetsRequest);
            log.debug("changeResourceRecordSetsResponse: {}", changeResourceRecordSetsResponse);
        }
    }

    private String getHostedZoneId(Route53Client route53Client) {
        ListHostedZonesByNameRequest listHostedZonesByNameRequest = ListHostedZonesByNameRequest.builder()
                .dnsName(route53DnsProperties.getRoute53Zone())
                .maxItems("1")
                .build();
        log.debug("listHostedZonesByNameRequest: {}", listHostedZonesByNameRequest);

        ListHostedZonesByNameResponse listHostedZonesByNameResponse =
                route53Client.listHostedZonesByName(listHostedZonesByNameRequest);
        log.debug("listHostedZonesByNameResponse: {}", listHostedZonesByNameResponse);

        List<HostedZone> hostedZones = listHostedZonesByNameResponse.hostedZones();
        if (hostedZones.size() != 1) {
            throw new IllegalStateException("expected 1 hosted zone but got: " + hostedZones);
        }

        HostedZone hostedZone = hostedZones.get(0);
        if (!hostedZone.name().equals(route53DnsProperties.getRoute53Zone() + ".")) {
            throw new IllegalStateException("expected hosted zone with name " + route53DnsProperties.getRoute53Zone() + ". but got: " + hostedZone.name());
        }

        String hostedZoneId = hostedZone.id().replace("/hostedzone/", "");
        log.debug("hostedZoneId: {}", hostedZoneId);
        // TODO: cache this
        return hostedZoneId;
    }

    private static Change upsert(String domain, Set<String> ips) {
        return Change.builder()
                .action(ChangeAction.UPSERT)
                .resourceRecordSet(ResourceRecordSet.builder()
                        .type(RRType.A)
                        .name(domain)
                        .resourceRecords(ips.stream()
                                .map(ip -> ResourceRecord.builder().value(ip).build())
                                .toList())
                        .ttl(60L)
                        .build()).build();
    }
}

//@Value("${adhoc.dns-route53.aws-access-key-id}")
//private String awsAccessKeyId;
//
//@Value("${adhoc.dns-route53.aws-secret-access-key}")
//private String awsSecretAccessKey;

//StaticCredentialsProvider
//return AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey);
//return AwsCredentialsProviderChain.builder().credentialsProviders(
//        ContainerCredentialsProvider.builder().build(),
//        ProfileCredentialsProvider.builder().profileName("adhoc").build()).build();

//ListResourceRecordSetsRequest listResourceRecordSetsRequest = ListResourceRecordSetsRequest.builder()
//        .hostedZoneId(hostedZoneId)
//        .startRecordType(RRType.A)
//        .startRecordName(domain)
//        .build();
//log.info("listResourceRecordSetsRequest: {}", listResourceRecordSetsRequest);
//
//ListResourceRecordSetsResponse listResourceRecordSetsResponse = getRoute53Client()
//        .listResourceRecordSets(listResourceRecordSetsRequest);
//log.info("listResourceRecordSetsResponse: {}", listResourceRecordSetsResponse);

//TestDnsAnswerResponse testDnsAnswerResponse = getRoute53Client()
//        .testDNSAnswer(TestDnsAnswerRequest.builder()
//                .hostedZoneId(hostedZoneId)
//                .recordType(RRType.A)
//                .recordName(domain).build());
//log.info("testDnsAnswerResponse: {}", testDnsAnswerResponse);
