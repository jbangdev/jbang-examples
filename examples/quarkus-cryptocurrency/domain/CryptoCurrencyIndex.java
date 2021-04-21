package domain;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.ext.web.client.WebClient;
import java.time.Duration;
import java.util.Set;
import org.eclipse.microprofile.config.ConfigProvider;

public class CryptoCurrencyIndex {

    private static final int TIMEOUT = 30;
    private static final int RETRIES_AMOUNT = 3;
    private String cryptoCurrencyPath;

    public Set<String> index;
    public CryptoCurrencyIndex() {
        cryptoCurrencyPath = ConfigProvider.getConfig().getValue("app.cryptoPath.provider.path", String.class);
    }

    private CryptoCurrencyIndex(Set<String> index) {
        this.index = index;
    }

    public Uni<CryptoCurrencyIndex> retrieveCryptoCurrenciesNames(WebClient client) {
        return client.getAbs(cryptoCurrencyPath + "/data/all/coinlist")
                .send()
                .map(resp -> new CryptoCurrencyIndex(resp.bodyAsJsonObject().getJsonObject("Data").fieldNames()))
                .ifNoItem().after(Duration.ofSeconds(TIMEOUT)).fail()
                .onFailure().retry().atMost(RETRIES_AMOUNT);
    }
}
