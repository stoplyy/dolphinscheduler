package org.apache.dolphinscheduler.api.platform;

import java.util.concurrent.TimeUnit;

import org.apache.dolphinscheduler.api.platform.facade.StellarOpsOpenApiEndpointFeign;

import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.Request;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;

public class StellarOpsOpenApiFactory {

    private Client client;
    private Encoder encoder;
    private Decoder decoder;
    private Contract contract;

    public StellarOpsOpenApiFactory() {
        this.client = new OkHttpClient(); // 使用 OkHttp 客户端
        this.encoder = new JacksonEncoder(); // 使用 Jackson 作为 JSON 编码器
        this.decoder = new JacksonDecoder(); // 使用 Jackson 作为 JSON 解码器
        this.contract = new Contract.Default(); // 使用默认的合约
    }

    public StellarOpsOpenApiEndpointFeign createClient(String appId) {
        Feign.Builder builder = Feign.builder();
        builder.client(client)
                .encoder(encoder)
                .decoder(decoder)
                .contract(contract)
                .options(new Request.Options(5, TimeUnit.SECONDS, 20, TimeUnit.SECONDS, true));

        return builder.target(StellarOpsOpenApiEndpointFeign.class, "http://" + appId.replaceFirst("http://", ""));
    }
}