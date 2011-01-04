The keystore contains a private key and its associated self-signed certificate
that can be used by SSL. This keystore is provided to be used only for testing this software.
In non-test environments it is highly recommended to use real certificates signed by a Certification Authority (CA).

The keystore was generated with the following command:

	$ keytool -keystore keystore -alias jetty -genkey -keyalg RSA
	Enter keystore password: password
	Re-enter new password: password
	What is your first and last name?
	  [Unknown]: vysper.org
	What is the name of your organizational unit?
	  [Unknown]:
	What is the name of your organization?
	  [Unknown]:  Vysper
	What is the name of your City or Locality?
	  [Unknown]:
	What is the name of your State or Province?
	  [Unknown]:
	What is the two-letter country code for this unit?
	  [Unknown]:
	Is CN=vysper.org, OU=Unknown, O=Vysper, L=Unknown, ST=Unknown, C=Unknown correct?
	  [no]: yes
	Enter key password for <jetty>
		(RETURN if same as keystore password):

The password for the keystore and for the key is "password" and the Common Name (CN) is "vysper.org".
The CN must be the domain name used by the clients to connect to the Vysper server (otherwise
the browser will warn the user that the certificate is issued for another domain name)

Using the self-signed certificate (from the keystore) will make the web browser warn you that the certificate is self-signed,
but for testing purposes you can add an exception for the browser to accept it.

More information about keytool: http://java.sun.com/javase/6/docs/technotes/tools/windows/keytool.html