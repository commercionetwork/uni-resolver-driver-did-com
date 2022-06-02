package uniresolver.driver.did.com.beans;

import java.util.List;

public class DidDocument {
	public List<String> context;
	public List<Object> verificationMethod;
	public List<Object> authentication;
	public List<Object> assertionMethod;
	public List<Object> keyAgreement;
	public List<Object> service;
}