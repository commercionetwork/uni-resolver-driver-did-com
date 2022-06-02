package uniresolver.driver.did.sov;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import foundation.identity.did.DID;
import foundation.identity.did.DIDDocument;
import foundation.identity.did.Service;
import foundation.identity.did.VerificationMethod;
import foundation.identity.did.parser.ParserException;
import uniresolver.ResolutionException;
import uniresolver.driver.Driver;
import uniresolver.driver.did.sov.beans.DidDocument;
import uniresolver.driver.did.sov.beans.Identity;
import uniresolver.driver.did.sov.beans.IdentityData;
import uniresolver.result.ResolveDataModelResult;

public class DidSovDriver implements Driver {

	public static void main(String[] args) throws ResolutionException, IllegalArgumentException, ParserException {
		DidSovDriver driver = new DidSovDriver();
		DID did1 = DID.fromString("did:com:109l7hvxq4kk0mtarfcl3gy3cdxuypdmt6j50ln");
		ResolveDataModelResult rdm1 = driver.resolve(did1, Map.of());
		System.out.println(rdm1);
	}

	private final static Logger LOGGER = LoggerFactory.getLogger(DidSovDriver.class);
	public final static Pattern DID_COM_PATTERN = Pattern.compile("^did:com:([0-9a-hj-np-z]{38,39})$"); // TODO: verify
	private final static Gson GSON = new Gson();

	private final Map<String, Object> properties;

	public DidSovDriver(Map<String, Object> properties) {
		this.properties = properties;
	}

	public DidSovDriver() {
		this(getPropertiesFromEnvironment());
	}

	private static Map<String, Object> getPropertiesFromEnvironment() {
		if (LOGGER.isDebugEnabled()) LOGGER.debug("Loading from environment: " + System.getenv());

		Map<String, Object> properties = new HashMap<>();

		try {
			String env_libIndyPath = System.getenv("uniresolver_driver_did_sov_libIndyPath");
			String env_poolConfigs = System.getenv("uniresolver_driver_did_sov_poolConfigs");
			String env_poolVersions = System.getenv("uniresolver_driver_did_sov_poolVersions");
			String env_walletNames = System.getenv("uniresolver_driver_did_sov_walletNames");
			String env_submitterDidSeeds = System.getenv("uniresolver_driver_did_sov_submitterDidSeeds");
			String env_genesisTimestamps = System.getenv("uniresolver_driver_did_sov_genesisTimestamps");

			if (env_libIndyPath != null) properties.put("libIndyPath", env_libIndyPath);
			if (env_poolConfigs != null) properties.put("poolConfigs", env_poolConfigs);
			if (env_poolVersions != null) properties.put("poolVersions", env_poolVersions);
			if (env_walletNames != null) properties.put("walletNames", env_walletNames);
			if (env_submitterDidSeeds != null) properties.put("submitterDidSeeds", env_submitterDidSeeds);
			if (env_genesisTimestamps != null) properties.put("genesisTimestamps", env_genesisTimestamps);
		}
		catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		return properties;
	}

	@Override
	public ResolveDataModelResult resolve(DID did, Map<String, Object> resolveOptions) throws ResolutionException {
		System.out.println("Trying to resolve " + did);

		try {
			Matcher matcher = DID_COM_PATTERN.matcher(did.getDidString());
			if (!matcher.matches()) {
				LOGGER.debug("The DID did not match its expected format");
				return null;
			}

			String request = "https://lcd-devnet.commercio.network/commercionetwork/did/" + did.getDidString() + "/identities";
			String response = sendGetRequest(request);
			IdentityData identity = GSON.fromJson(response, Identity.class).identity;
			DidDocument responseDidDocument = identity.didDocument;
			List<URI> contexts = responseDidDocument.context.stream()
					// the following context is added by default
					.filter(context -> !"https://www.w3.org/ns/did/v1".equals(context))
					.map(URI::create)
					.collect(Collectors.toList());

			DIDDocument didDocument = DIDDocument.builder()
					.contexts(contexts)
					.id(did.toUri())
					.verificationMethods(intoVerificationMethods(responseDidDocument.verificationMethod))
					.authenticationVerificationMethods(intoVerificationMethods(responseDidDocument.authentication))
					.assertionMethodVerificationMethods(intoVerificationMethods(responseDidDocument.assertionMethod))
					.keyAgreementVerificationMethods(intoVerificationMethods(responseDidDocument.keyAgreement))
					.services(intoServices(responseDidDocument.service))
					.build();

			return ResolveDataModelResult.build(null, didDocument, identity.metadata);
		}
		catch (Exception e) {
			throw new ResolutionException(e.getMessage(), e);
		}
	}

	private static List<VerificationMethod> intoVerificationMethods(List<Object> list) {
		return intoBeans(list, VerificationMethod::fromMap);
	}

	private static List<Service> intoServices(List<Object> list) {
		return intoBeans(list, Service::fromMap);
	}

	@SuppressWarnings("unchecked")
	private static <T> List<T> intoBeans(List<Object> list, Function<Map<String, Object>, T> mapper) {
		return list.stream()
			.map(object -> (Map<String, Object>) object)
			.map(mapper)
			.collect(Collectors.toList());
	}

	private String sendGetRequest(String request) throws IOException {
		System.out.println("Sending request " + request);

		URL url = new URL(request);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		try {
			con.setRequestMethod("GET");
			con.setRequestProperty("Content-Type", "application/json; UTF-8");
			con.setRequestProperty("Accept", "application/json");
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);

			return readFrom(con);
		}
		finally {
			con.disconnect();
		}
	}

	/**
	 * Reads the response from the given connection.
	 * 
	 * @param connection the connection
	 * @return the response
	 * @throws IOException if the response couldn't be read
	 */
	private static String readFrom(HttpURLConnection connection) throws IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
			return br.lines().collect(Collectors.joining());
		}
	}

	@Override
	public Map<String, Object> properties() {
		return properties;
	}
}