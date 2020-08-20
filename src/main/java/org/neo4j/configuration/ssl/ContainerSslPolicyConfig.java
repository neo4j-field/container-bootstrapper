package org.neo4j.configuration.ssl;

import org.neo4j.configuration.Description;
import org.neo4j.configuration.GroupSetting;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.org.neo4j.configuration.AssetUri;
import org.neo4j.org.neo4j.configuration.ContainerSettingValueParsers;
import org.neo4j.string.SecureString;

import java.nio.file.Path;
import java.util.List;

import static org.neo4j.configuration.GraphDatabaseSettings.neo4j_home;
import static org.neo4j.configuration.SettingValueParsers.*;

public class ContainerSslPolicyConfig extends GroupSetting {
    @Description("Private PKCS#8 key in PEM format.")
    public final Setting<AssetUri> remote_private_key;

    @Description("X.509 certificate (chain) of this server in PEM format.")
    public final Setting<AssetUri> remote_public_certificate;

    public final Setting<Boolean> enabled = getBuilder("enabled", BOOL, Boolean.FALSE).build();

    @Description("The mandatory base directory for cryptographic objects of this policy." +
            " It is also possible to override each individual configuration with absolute paths.")
    public final Setting<Path> base_directory;

    @Description("Path to directory of CRLs (Certificate Revocation Lists) in PEM format.")
    public final Setting<Path> revoked_dir;

    @Description("Makes this policy trust all remote parties." +
            " Enabling this is not recommended and the trusted directory will be ignored.")
    public final Setting<Boolean> trust_all = getBuilder("trust_all", BOOL, false).build();

    @Description("Client authentication stance.")
    public final Setting<ClientAuth> client_auth;

    @Description("Restrict allowed TLS protocol versions.")
    public final Setting<List<String>> tls_versions = getBuilder("tls_versions", listOf(STRING), List.of("TLSv1.2")).build();

    @Description("Restrict allowed ciphers. " +
            "Valid values depend on JRE and SSL however some examples can be found here " +
            "https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#jsse-cipher-suite-names")
    public final Setting<List<String>> ciphers = getBuilder("ciphers", listOf(STRING), null).build();

    @Description("When true, this node will verify the hostname of every other instance it connects to by comparing the address it used to connect with it " +
            "and the patterns described in the remote hosts public certificate Subject Alternative Names")
    public final Setting<Boolean> verify_hostname = getBuilder("verify_hostname", BOOL, false).build();

    @Description("The passphrase for the private key.")
    public final Setting<SecureString> private_key_password = getBuilder("private_key_password", SECURE_STRING, null).build();

    @Description("Path to directory of X.509 certificates in PEM format for trusted parties.")
    public final Setting<Path> trusted_dir;

    private SslPolicyScope scope;

    public static ContainerSslPolicyConfig forScope(SslPolicyScope scope) {
        return new ContainerSslPolicyConfig(scope.name());
    }

    private ContainerSslPolicyConfig(String scopeString) {
        super(scopeString.toLowerCase());
        scope = SslPolicyScope.fromName(scopeString);
        if (scope == null) {
            throw new IllegalArgumentException("SslPolicy can not be created for scope: " + scopeString);
        }

        remote_private_key = getBuilder("remote_private_key", ContainerSettingValueParsers.ASSET_URI, null).build();
        remote_public_certificate = getBuilder("remote_public_certificate", ContainerSettingValueParsers.ASSET_URI, null).build();

        client_auth = getBuilder("client_auth", ofEnum(ClientAuth.class), scope.authDefault).build();
        base_directory = getBuilder("base_directory", PATH, Path.of(scope.baseDir)).setDependency(neo4j_home).immutable().build();
        revoked_dir = getBuilder("revoked_dir", PATH, Path.of("revoked")).setDependency(base_directory).build();
        trusted_dir = getBuilder("trusted_dir", PATH, Path.of("trusted")).setDependency(base_directory).build();
    }

    public ContainerSslPolicyConfig() //For serviceloading
    {
        this("testing");
    }

    @Override
    public String getPrefix() {
        return "dbms.ssl.policy";
    }

    public SslPolicyScope getScope() {
        return scope;
    }

}
