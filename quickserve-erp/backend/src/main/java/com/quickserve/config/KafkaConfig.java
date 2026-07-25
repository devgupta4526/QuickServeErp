package com.quickserve.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

/** Kafka topic declarations — disabled in mock profile. */
@Configuration
@Profile("!mock")
public class KafkaConfig {

    @Bean public NewTopic orderPlaced()           { return TopicBuilder.name("order.placed").partitions(3).replicas(1).build(); }
    @Bean public NewTopic orderStatusChanged()    { return TopicBuilder.name("order.status.changed").partitions(3).replicas(1).build(); }
    @Bean public NewTopic paymentProcessed()      { return TopicBuilder.name("payment.processed").partitions(3).replicas(1).build(); }
    @Bean public NewTopic stockDeducted()         { return TopicBuilder.name("stock.deducted").partitions(3).replicas(1).build(); }
    @Bean public NewTopic stockLow()              { return TopicBuilder.name("stock.low").partitions(3).replicas(1).build(); }
    @Bean public NewTopic invoiceGenerated()      { return TopicBuilder.name("invoice.generated").partitions(3).replicas(1).build(); }
    @Bean public NewTopic whatsappSend()          { return TopicBuilder.name("whatsapp.send").partitions(3).replicas(1).build(); }
    @Bean public NewTopic whatsappRetry()         { return TopicBuilder.name("whatsapp.retry").partitions(3).replicas(1).build(); }
    @Bean public NewTopic whatsappIncoming()      { return TopicBuilder.name("whatsapp.incoming").partitions(3).replicas(1).build(); }
    @Bean public NewTopic payrollProcessed()      { return TopicBuilder.name("payroll.processed").partitions(3).replicas(1).build(); }
    @Bean public NewTopic campaignStart()         { return TopicBuilder.name("campaign.start").partitions(3).replicas(1).build(); }
}
