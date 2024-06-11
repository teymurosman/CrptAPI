package ru.teymurosman;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class CrptApi {
    private final int requestLimit;
    private final ScheduledExecutorService scheduler;
    private final HttpClient httpClient;
    private final String apiUrl;
    private volatile int requests;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.httpClient = HttpClient.newHttpClient();
        this.apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";
        this.requests = 0;

        scheduler.scheduleAtFixedRate(() -> requests = 0, 0, 1, timeUnit);
    }

    public synchronized void createDocument(Document document, String signature) throws InterruptedException {
        while (requests >= requestLimit) {
            wait();
        }

        requests++;
        try {
            sendRequest(document, signature);
        } finally {
            notifyAll();
        }
    }

    private void sendRequest(Document document, String signature) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String requestBody = objectMapper.writeValueAsString(document);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response: " + response.body());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Getter
    @Setter
    public static class Document {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type = "LP_INTRODUCE_GOODS";
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private Product[] products;
        private String reg_date;
        private String reg_number;

        @Getter
        @Setter
        public static class Description {
            private String participantInn;
        }

        @Getter
        @Setter
        public static class Product {
            private String certificate_document;
            private String certificate_document_date;
            private String certificate_document_number;
            private String owner_inn;
            private String producer_inn;
            private String production_date;
            private String tnved_code;
            private String uit_code;
            private String uitu_code;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        CrptApi api = new CrptApi(TimeUnit.MINUTES, 10);

        Document.Description description = new Document.Description();
        description.participantInn = "1234567890";

        Document.Product product = new Document.Product();
        product.certificate_document = "cert_doc";
        product.certificate_document_date = "2020-01-23";
        product.certificate_document_number = "cert_number";
        product.owner_inn = "owner_inn";
        product.producer_inn = "producer_inn";
        product.production_date = "2020-01-23";
        product.tnved_code = "tnved_code";
        product.uit_code = "uit_code";
        product.uitu_code = "uitu_code";

        Document document = new Document();
        document.description = description;
        document.doc_id = "doc_id";
        document.doc_status = "doc_status";
        document.importRequest = true;
        document.owner_inn = "owner_inn";
        document.participant_inn = "participant_inn";
        document.producer_inn = "producer_inn";
        document.production_date = "2020-01-23";
        document.production_type = "production_type";
        document.products = new Document.Product[]{product};
        document.reg_date = "2020-01-23";
        document.reg_number = "reg_number";

        api.createDocument(document, "signature");
    }
}

