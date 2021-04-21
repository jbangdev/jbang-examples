package domain;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.client.WebClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.ConfigProvider;

public class CryptoCurrencyValues {

    private static final int TIMEOUT = 30;
    private static final int RETRIES_AMOUNT = 3;

    private String cryptoCurrencyPath;

    public String name;
    public double bitcoin;
    public double uniteStateDollar;
    public double euro;

    public CryptoCurrencyValues() {
        cryptoCurrencyPath = ConfigProvider.getConfig().getValue("app.cryptoPath.provider.path", String.class);
    }

    private CryptoCurrencyValues(String currencyName, double btc, double usd, double eur) {
        this.name = currencyName;
        this.bitcoin = btc;
        this.uniteStateDollar = usd;
        this.euro = eur;
    }

    private CryptoCurrencyValues from(String name, JsonObject json) {
        Double btcValue = json.getJsonObject(name).getDouble(Currency.BTC.name());
        Double usdValue = json.getJsonObject(name).getDouble(Currency.USD.name());
        Double eurValue = json.getJsonObject(name).getDouble(Currency.EUR.name());

        return new CryptoCurrencyValues(name, btcValue, usdValue, eurValue);
    }

    public Uni<CryptoCurrencyValues> getCurrencyValue(WebClient client, String currency) {
        String currencies = Arrays.stream(Currency.values()).map(m -> m.name()).collect(Collectors.joining(","));
        return client.getAbs(cryptoCurrencyPath + "/data/pricemulti?fsyms=" + currency + "&tsyms=" + currencies)
                .send()
                .map(resp -> from(currency, resp.bodyAsJsonObject()))
                .ifNoItem().after(Duration.ofSeconds(TIMEOUT)).fail()
                .onFailure().retry().atMost(RETRIES_AMOUNT);
    }
}
