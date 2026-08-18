package flashsalesystem.paymentservice.controllers;

import flashsalesystem.paymentservice.dtos.PaymentRequest;
import flashsalesystem.paymentservice.enums.PaymentResult;
import flashsalesystem.paymentservice.services.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController
{
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService)
    {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Void> processPayment(@RequestBody PaymentRequest paymentRequest)
    {
        PaymentResult paymentResult = paymentService.processPayment(paymentRequest.orderId(), paymentRequest.amountDue());
        if(paymentResult == PaymentResult.SUCCESS) return ResponseEntity.ok().build();
        else if(paymentResult == PaymentResult.PAYMENT_FAILED) return ResponseEntity.status(HttpStatus.CONFLICT).build();
        else if(paymentResult == PaymentResult.PROCESSOR_ERROR) return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        else return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
