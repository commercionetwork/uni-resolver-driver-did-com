package uniresolver.driver.did.com.beans;

import java.util.List;
import java.util.Map;

public class DidDocument {
	public List<String> context;
	public List<Map<String,Object>> verificationMethod;
	public List<Map<String,Object>> authentication;
	public List<Map<String,Object>> assertionMethod;
	public List<Map<String,Object>> keyAgreement;
	public List<Map<String,Object>> service;
}