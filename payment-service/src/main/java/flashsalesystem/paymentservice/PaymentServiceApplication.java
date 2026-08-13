package flashsalesystem.paymentservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
public class PaymentServiceApplication
{
    private static final Logger log = LoggerFactory.getLogger(PaymentServiceApplication.class);

    public static void main(String[] args)
    {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner logFailureRateOnStartup(@Value("${app.failure-rate}") double failureRate)
    {
        return args -> log.info("Failure rate from config server is {}", failureRate);
    }

}
