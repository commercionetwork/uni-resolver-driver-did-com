package uniresolver.driver.did.com;

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
import uniresolver.driver.did.com.beans.DidDocument;
import uniresolver.driver.did.com.beans.Identity;
import uniresolver.driver.did.com.beans.IdentityData;
import uniresolver.result.ResolveDataModelResult;

public class DidComDriver implements Driver {
	public final static Pattern DID_COM_PATTERN = Pattern.compile("^did:com:([0-9a-hj-np-z]{38,39})$"); // TODO: verify

	/**
	 * The key used in the properties provided to this application, in order to specify the network to connect to.
	 */
	public final static String NETWORK_KEY_IN_PROPERTIES = "uniresolver_driver_did_com_network";

	private final static Gson GSON = new Gson();
	private final static Logger LOGGER = LoggerFactory.getLogger(DidComDriver.class);

	/**
	 * The properties passed to this driver.
	 */
	private final Map<String, Object> properties;

	/**
	 * The network to connect to.
	 */
	private final String network;

	public static void main(String[] args) throws ResolutionException, IllegalArgumentException, ParserException {
		DID did = DID.fromString("did:com:109l7hvxq4kk0mtarfcl3gy3cdxuypdmt6j50ln");
		ResolveDataModelResult rdm1 = new DidComDriver().resolve(did, Map.of());
		System.out.println(rdm1);
	}

	/**
	 * Builds a universal resolver driver for the Commercio network.
	 * 
	 * @param properties the properties passed to the driver.
	 */
	public DidComDriver(Map<String, Object> properties) {
		this.properties = properties;
		this.network = selectedNetwork();
	}

	/**
	 * Builds a universal resolver driver for the Commercio network.
	 * Extracts the properties from the environment.
	 */
	public DidComDriver() {
		this(getPropertiesFromEnvironment());
	}

	@Override
	public ResolveDataModelResult resolve(DID did, Map<String, Object> resolveOptions) throws ResolutionException {
		logAsDebug("Trying to resolve " + did);

		try {
			Matcher matcher = DID_COM_PATTERN.matcher(did.getDidString());
			if (!matcher.matches()) {
				logAsDebug("The DID doesn't match its expected format");
				return null;
			}

			String request = network + "/commercionetwork/did/" + did.getDidString() + "/identities";
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

			logAsDebug("Resolved " + did);
			return ResolveDataModelResult.build(null, didDocument, identity.metadata);
		}
		catch (Exception e) {
			throw new ResolutionException(e.getMessage(), e);
		}
	}

	@Override
	public Map<String, Object> properties() {
		return properties;
	}

	/**
	 * Infers the Commercio network endpoint to contact. This is specified in the properties
	 * passed to this driver, and defaults to https://lcd-devnet.commercio.network.
	 * 
	 * @return the endpoint
	 */
	private String selectedNetwork() {
		Object network = properties.get(NETWORK_KEY_IN_PROPERTIES);
		if (network instanceof String)
			return (String) network;
		else
			return "https://lcd-devnet.commercio.network";
	}

	private static Map<String, Object> getPropertiesFromEnvironment() {
		logAsDebug("Loading from environment: " + System.getenv());
	
		Map<String, Object> properties = new HashMap<>();
	
		try {
			String network = System.getenv(NETWORK_KEY_IN_PROPERTIES);
			if (network != null)
				properties.put(NETWORK_KEY_IN_PROPERTIES, network);
		}
		catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	
		return properties;
	}

	private static void logAsDebug(String message) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(message);
	}

	private static List<VerificationMethod> intoVerificationMethods(List<Map<String,Object>> list) {
		return intoBeans(list, VerificationMethod::fromMap);
	}

	private static List<Service> intoServices(List<Map<String,Object>> list) {
		return intoBeans(list, Service::fromMap);
	}

	private static <T> List<T> intoBeans(List<Map<String,Object>> list, Function<Map<String, Object>, T> mapper) {
		return list.stream()
			.map(mapper)
			.collect(Collectors.toList());
	}

	/**
	 * Sends a GET request to the Commercio network.
	 * 
	 * @param request the request
	 * @return the response
	 * @throws IOException if the request failed
	 */
	private static String sendGetRequest(String request) throws IOException {
		logAsDebug("Sending request " + request);

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
}