package name.w.Zimbra.RushFilesZimlet.Tests;

import name.w.Zimbra.RushFilesZimlet.RushFiles.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestMethodOrder( MethodOrderer.Alphanumeric.class )
public class APITest
{
    private final String primaryDomain = "primdomain.com";
    private final String username = "user@domain.com";
    private final String domainToken
        = "%7B%22Token%22%3A%221000%3A9zo%2BiNiS48XnsbErbyy%2FlQ%3D%3D%3A8VSjpLlO1S01CwOZ%3A86zFFkR%2FfGG8TP0WoY7HmA66PIg8QNUNwAlV1YwIb0%2FMlPcFSTfJUdVpKlsLQiHi%3AtpgCzKgBhjnWsh6qWUBBXLasA%2F32azwj7ixFHynK%2F6Q%3D%22%2C%22KeyId%22%3A%2242bd1e5eca0f4ccd95e8572de9d4c566%22%2C%22Timestamp%22%3A%222020-05-04T09%3A37%3A10.1972909Z%22%2C%22Thumbprint%22%3Anull%7D";
    private final String apiHost = "https://clientgateway.primdomain.com";

    private final Authenticator authenticator = new Authenticator();
    private NetworkDriverMock network;

    @BeforeEach
    public void beforeEach()
    {
        network = spy( NetworkDriverMock.class );
        authenticator.redefineNetworkDriver( network );
    }

    @Test
    public void testAuthorization() throws Exception
    {
        mockRequestResponseOnUrlContains(
            "https://global.rushfiles.com/getuserdomain.aspx?useremail=" + username,
            username + "," + primaryDomain
        );

        mockRequestResponseOnUrlMatches(
            apiHost + "\\/api\\/devices\\/[A-Fa-f0-9]{64}",
            "{\n" +
            "  \"Data\": {\n" +
            "    \"Id\": \"" + username + "\",\n" +
            "    \"Name\": \"device\",\n" +
            "    \"Os\": \"ios\",\n" +
            "    \"DeviceType\": 0\n" +
            "  },\n" +
            "  \"Message\": \"Ok.\"\n" +
            "}"
        );

        mockRequestResponseOnUrlContains(
            apiHost + "/api/domaintokens",
            "{\n" +
            "  \"Data\": {\n" +
            "    \"DomainTokens\": [\n" +
            "      {\n" +
            "        \"DomainUrl\": \"" + primaryDomain + "\",\n" +
            "        \"DomainToken\": \"" + domainToken + "\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"DomainUrl\": \"rushfiles.net\",\n" +
            "        \"DomainToken\": \"%7B%22Token%22%3A%221000%3AWcV%2BtvmYeovB0rDH0NYNTg%3D%3D%3AdP3rqG%2FNpDxTiLkt%3AxryTEyEJPT2nKKqmcjdUICznzo2I57inzbynczmiamjOU2cxPtM0kBT8j1QQd14z%3AZNoaEYM7Wwgguz5HuC7kgtwdxf4wEpvT3kltgmwAj6Y%3D%22%2C%22KeyId%22%3A%220747c5dd374c4aa1b4038c90170341f1%22%2C%22Timestamp%22%3A%222020-05-04T09%3A37%3A10.1972909Z%22%2C%22Thumbprint%22%3Anull%7D\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"WipeDevice\": false\n" +
            "  },\n" +
            "  \"Message\": \"Ok.\"\n" +
            "}"
        );

        final API rfUnauth = authenticator.unauthorized( username, "123456" );
        assertEquals( primaryDomain, rfUnauth.getPrimaryDomain() );
        assertEquals( domainToken, rfUnauth.getDomainToken() );
        assertEquals( username, rfUnauth.getUsername() );

        final API rfAuth = authenticator.authorized( primaryDomain, domainToken, username );
        assertEquals( primaryDomain, rfAuth.getPrimaryDomain() );
        assertEquals( domainToken, rfAuth.getDomainToken() );
        assertEquals( username, rfAuth.getUsername() );
    }

    @Test
    public void testFolderContentGetting() throws Exception
    {
        final String share = "any";
        final String internalName = "any";

        mockRequestResponseOnUrlContains(
            apiHost + "/api/shares/" + share + "/virtualfiles/" + internalName + "/children",
            "{\n" +
            "  \"Data\": [\n" +
            "    {\n" +
            "      \"IsFile\": true,\n" +
            "      \"InternalName\": \"c9d9c18cecc64940a45adb8890f61529\",\n" +
            "      \"ShareId\": \"17b8cd708c5f41f0a7d30a7230612de2\",\n" +
            "      \"PublicName\": \"Addressbook.png\",\n" +
            "    },\n" +
            "    {\n" +
            "      \"IsFile\": true,\n" +
            "      \"InternalName\": \"b6370bec1cb24eb181e64e857beec598\",\n" +
            "      \"ShareId\": \"17b8cd708c5f41f0a7d30a7230612de2\",\n" +
            "      \"PublicName\": \"Default.png\",\n" +
            "    }\n" +
            "  ],\n" +
            "  \"Message\": \"Ok.\"\n" +
            "}"
        );

        final VirtualFile[] files = authenticator
            .authorized( primaryDomain, domainToken, username )
            .getFolderContent( share, internalName );

        assertEquals( 2, files.length );

        assertTrue( files[ 0 ].IsFile );
        assertEquals( "c9d9c18cecc64940a45adb8890f61529", files[ 0 ].InternalName );
        assertEquals( "17b8cd708c5f41f0a7d30a7230612de2", files[ 0 ].ShareId );
        assertEquals( "Addressbook.png", files[ 0 ].PublicName );

        assertTrue( files[ 1 ].IsFile );
        assertEquals( "b6370bec1cb24eb181e64e857beec598", files[ 1 ].InternalName );
        assertEquals( "17b8cd708c5f41f0a7d30a7230612de2", files[ 1 ].ShareId );
        assertEquals( "Default.png", files[ 1 ].PublicName );
    }

    @Test
    public void testSharesGetting() throws Exception
    {
        mockRequestResponseOnUrlContains(
            apiHost + "/api/users/shares?userId=" + username,
            "{\n" +
            "  \"Data\": [\n" +
            "    {\n" +
            "      \"Id\": \"17b8cd708c5f41f0a7d30a7230612de2\",\n" +
            "      \"CompanyId\": \"39929886c09745e3bc98b9e85be7d0fb\",\n" +
            "      \"Name\": \"Comp Inc\",\n" +
            "    },\n" +
            "    {\n" +
            "      \"Id\": \"d94f8ed4c56e4f318edb41e5da8b064a\",\n" +
            "      \"CompanyId\": \"ca10b965-3b9f-4e5a-96a6-f10b3acea1b8\",\n" +
            "      \"Name\": \"Home folder\",\n" +
            "    }\n" +
            "  ],\n" +
            "  \"Message\": \"Ok.\"\n" +
            "}"
        );

        final Share[] files = authenticator.authorized( primaryDomain, domainToken, username ).getShares();

        assertEquals( 2, files.length );

        assertEquals( "17b8cd708c5f41f0a7d30a7230612de2", files[ 0 ].Id );
        assertEquals( "Comp Inc", files[ 0 ].Name );
        assertEquals( "39929886c09745e3bc98b9e85be7d0fb", files[ 0 ].CompanyId );

        assertEquals( "d94f8ed4c56e4f318edb41e5da8b064a", files[ 1 ].Id );
        assertEquals( "Home folder", files[ 1 ].Name );
        assertEquals( "ca10b965-3b9f-4e5a-96a6-f10b3acea1b8", files[ 1 ].CompanyId );
    }

    @Test
    public void testShareContentGetting() throws Exception
    {
        final String shareId = "any";

        mockRequestResponseOnUrlContains(
            apiHost + "/api/shares/" + shareId + "/children",
            "{\n" +
            "  \"Data\": [\n" +
            "    {\n" +
            "      \"IsFile\": false,\n" +
            "      \"InternalName\": \"a42a0704af704efd83e515f97cac7b70\",\n" +
            "      \"ShareId\": \"17b8cd708c5f41f0a7d30a7230612de2\",\n" +
            "      \"PublicName\": \"cats\",\n" +
            "    },\n" +
            "    {\n" +
            "      \"IsFile\": true,\n" +
            "      \"InternalName\": \"b54e638b67664c688dd0cf28537bf191\",\n" +
            "      \"ShareId\": \"17b8cd708c5f41f0a7d30a7230612de2\",\n" +
            "      \"PublicName\": \"testfile.txt\",\n" +
            "    }\n" +
            "  ],\n" +
            "  \"Message\": \"Ok.\"\n" +
            "}"
        );

        final VirtualFile[] files = authenticator
            .authorized( primaryDomain, domainToken, username )
            .getShareContent( shareId );

        assertEquals( 2, files.length );

        assertFalse( files[ 0 ].IsFile );
        assertEquals( "a42a0704af704efd83e515f97cac7b70", files[ 0 ].InternalName );
        assertEquals( "17b8cd708c5f41f0a7d30a7230612de2", files[ 0 ].ShareId );
        assertEquals( "cats", files[ 0 ].PublicName );

        assertTrue( files[ 1 ].IsFile );
        assertEquals( "b54e638b67664c688dd0cf28537bf191", files[ 1 ].InternalName );
        assertEquals( "17b8cd708c5f41f0a7d30a7230612de2", files[ 1 ].ShareId );
        assertEquals( "testfile.txt", files[ 1 ].PublicName );
    }

    @ParameterizedTest( name = "Create permanent link: {0} :: Does link already exists: {1} :: Is existed link permanent: {2} :: Should create new one instead of return existed: {3}" )
    @CsvSource( {
        "true, true, true, false",
        "true, false, false, true",
        "true, true, false, true",
        "false, true, false, true",
        "false, false, false, true",
        "false, true, true, true",
    } )
    public void testPublicLinkCreation( final boolean createPermanentLinkRequest,
                                        final boolean doesLinkExists, final boolean isExistedLinkPermanent,
                                        final boolean shouldBeCreatedNewLink )
        throws Exception
    {
        final String existedLinkId = "existed-link-id";
        final String createdLinkId = "created-link-id";
        final int creationLinkExpirationTime;
        final String existsLinkExpirationTime;
        final String linkIdShouldBeReturned;

        if( createPermanentLinkRequest ) {
            creationLinkExpirationTime = 0;
        }
        else {
            creationLinkExpirationTime = 60;
        }

        if( isExistedLinkPermanent ) {
            existsLinkExpirationTime = "3155378975999999999";
        }
        else {
            existsLinkExpirationTime = "60";
        }

        if( shouldBeCreatedNewLink ) {
            linkIdShouldBeReturned = createdLinkId;
        }
        else {
            linkIdShouldBeReturned = existedLinkId;
        }

        mockRequestResponseOnUrlContains(
            apiHost + "/api/publiclinks",
            "{\n" +
            "  \"Data\": {\n" +
            "    \"FullLink\": \"https://" + primaryDomain + "/client/publiclink.aspx?id=" + createdLinkId + "\"\n" +
            "  },\n" +
            "  \"Message\": \"Public link has been created.\"\n" +
            "}"
        );

        if( doesLinkExists ) {
            mockRequestResponseOnUrlContains(
                apiHost + "/api/shares/any/virtualfiles/any",
                "{\n" +
                "  \"Data\": [\n" +
                "    {\n" +
                "      \"Id\": \"" + existedLinkId + "\",\n" +
                "      \"ExpirationTimeUtc\": " + existsLinkExpirationTime + "\n" +
                "    }\n" +
                "  ],\n" +
                "  \"Message\": \"Ok.\"\n" +
                "}"
            );
        }
        else {
            mockRequestResponseOnUrlContains(
                apiHost + "/api/shares/any/virtualfiles/any",
                "{\n" +
                "  \"Data\": [],\n" +
                "  \"Message\": \"Ok.\"\n" +
                "}"
            );
        }

        final String createdLink = authenticator
            .authorized( primaryDomain, domainToken, username )
            .createPublicLink( "any", "any", creationLinkExpirationTime, 0, "", null
            );

        assertEquals(
            "https://" + primaryDomain + "/client/publiclink.aspx?id=" + linkIdShouldBeReturned, createdLink );
    }

    private void mockRequestResponseOnUrlContains( final String url, final String response ) throws APIException
    {
        doReturn( response )
            .when( network )
            .request( Mockito.contains( url ), Mockito.any(), Mockito.any(), Mockito.any() )
        ;
    }

    private void mockRequestResponseOnUrlMatches( final String urlRegex, final String response ) throws APIException
    {
        doReturn( response )
            .when( network )
            .request( Mockito.matches( urlRegex ), Mockito.any(), Mockito.any(), Mockito.any() )
        ;
    }
}
