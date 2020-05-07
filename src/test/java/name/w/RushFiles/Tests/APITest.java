package name.w.RushFiles.Tests;

import name.w.RushFiles.API;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;

@RunWith( PowerMockRunner.class )
@PrepareForTest( API.class )
public class APITest
{
    @Test
    public void testPrimaryDomainGetting() throws Exception
    {
        mockRequestReturn( "email@domain.com,primdomain.com" );
        assertEquals( "primdomain.com", API.getPrimaryDomain( "email@domain.com" ) );
    }

    @Test
    public void testDeviceRegistration() throws Exception
    {
        mockRequestReturn(
            "{\n" +
            "  \"Data\": {\n" +
            "    \"Id\": \"hopster1222\",\n" +
            "    \"Name\": \"device\",\n" +
            "    \"Os\": \"ios\",\n" +
            "    \"DeviceType\": 0\n" +
            "  },\n" +
            "  \"Message\": \"Ok.\"\n" +
            "}"
        );
        assertEquals( 64, API.registerDevice( "any", "any", "any", "any", "any", 0 ).length() );
    }

    @Test
    public void testDomainTokenGeneration() throws Exception
    {
        mockRequestReturn(
            "{\n" +
            "  \"Data\": {\n" +
            "    \"DomainTokens\": [\n" +
            "      {\n" +
            "        \"DomainUrl\": \"primdomain.com\",\n" +
            "        \"DomainToken\": \"%7B%22Token%22%3A%221000%3A9zo%2BiNiS48XnsbErbyy%2FlQ%3D%3D%3A8VSjpLlO1S01CwOZ%3A86zFFkR%2FfGG8TP0WoY7HmA66PIg8QNUNwAlV1YwIb0%2FMlPcFSTfJUdVpKlsLQiHi%3AtpgCzKgBhjnWsh6qWUBBXLasA%2F32azwj7ixFHynK%2F6Q%3D%22%2C%22KeyId%22%3A%2242bd1e5eca0f4ccd95e8572de9d4c566%22%2C%22Timestamp%22%3A%222020-05-04T09%3A37%3A10.1972909Z%22%2C%22Thumbprint%22%3Anull%7D\"\n" +
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

        assertTrue( API.generateDomainToken( "any", "any", "any", "any", 0, 0 )
                       .matches( "^.*Token.*?KeyId.*?Timestamp.*?Thumbprint.*$" ) );
    }

    @Test
    public void testPublicLinkCreation() throws Exception
    {
        mockRequestReturn(
            "{\n" +
            "  \"Data\": {\n" +
            "    \"FullLink\": \"https://cloudfile.jp/client/publiclink.aspx?id=VUVZk3KeZ7\"\n" +
            "  },\n" +
            "  \"Message\": \"Public link has been created.\"\n" +
            "}"
        );

        final String linkWithPassword = API.createPublicLink( "any", "any", "any", "any", 0, 0, "any", "any" );
        assertTrue( linkWithPassword.matches( "https://.*?/client/publiclink.aspx\\?id=.*" ) );

        final String linkWithoutPassword = API.createPublicLink( "any", "any", "any", "any", 0, 0, "any" );
        assertTrue( linkWithoutPassword.matches( "https://.*?/client/publiclink.aspx\\?id=.*" ) );
    }

    @Test
    public void testFolderContentGetting() throws Exception
    {
        mockRequestReturn(
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
            "}" );

        final API.VirtualFile[] files = API.getFolderContent( "any", "any", "any", "any" );

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
        mockRequestReturn(
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

        final API.Share[] files = API.getShares( "any", "any", "any" );

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
        mockRequestReturn(
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

        final API.VirtualFile[] files = API.getShareContent( "any", "any", "any" );

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

    private void mockRequestReturn( final String response ) throws Exception
    {
        PowerMockito.spy( API.class );
        PowerMockito
            .doReturn( response )
            .when( API.class, "request", Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any() )
        ;
    }
}
