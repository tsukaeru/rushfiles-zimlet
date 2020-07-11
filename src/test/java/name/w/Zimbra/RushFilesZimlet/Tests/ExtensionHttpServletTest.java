package name.w.Zimbra.RushFilesZimlet.Tests;

import name.w.Zimbra.RushFilesZimlet.ExtensionHttpServlet;
import name.w.Zimbra.RushFilesZimlet.RushFiles.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;

import static org.mockito.Mockito.*;

@TestMethodOrder( MethodOrderer.Alphanumeric.class )
public class ExtensionHttpServletTest
{
    protected static final String username = "hopster1222@gmail.com";
    protected static final String password = "L!-M/BBfol";

    protected static String primaryDomain;
    protected static String domainToken;
    protected static Share[] shares;
    protected static VirtualFile[] shareContent;

    @BeforeAll
    public static void beforeAll() throws APIException
    {
        final API api = new Authenticator().unauthorized( username, password );

        primaryDomain = api.getPrimaryDomain();
        domainToken = api.getDomainToken();
        shares = api.getShares();
        shareContent = api.getShareContent( shares[ 0 ].Id );
    }

    @Test
    public void testAuthorization()
    {
        testRequest(
            "authorize",
            "{'username': '" + username + "', 'password': '" + password + "' }",
            "\\{'domain_token':'.*Token.*Thumbprint.*','primary_domain':'cloudfile.jp','status':'success','username':'" +
            username + "'\\}"
        );
    }

    @Test
    public void testAuthorizationFail()
    {
        testRequest(
            "authorize",
            "{ 'username': 'any@any.com', 'password': 'any' }",
            "\\{'message':'Do not found primary domain for username any@any.com','status':'error'\\}"
        );
    }

    @Test
    public void testGetAllShares()
    {
        testRequest(
            "get_all_shares",
            new Cookie[] {
                new Cookie( "primary_domain", primaryDomain ),
                new Cookie( "domain_token", domainToken ),
                new Cookie( "username", username )
            },
            "\\{'objects':\\[\\{'CompanyId':'.*?','Id':'.*?','Name':'.*?'\\}.*?\\],'status':'success'\\}"
        );
    }

    @Test
    public void testGetShareContents()
    {
        testRequest(
            "get_share_contents",
            "{ 'ShareId': '" + shares[ 0 ].Id + "' }",
            new Cookie[] {
                new Cookie( "primary_domain", primaryDomain ),
                new Cookie( "domain_token", domainToken ),
                new Cookie( "username", username )
            },
            "\\{'objects':\\[\\{'IsFile':.*?,'ShareId':'.*?','PublicName':'.*?','InternalName':'.*?'\\}.*\\}\\],'status':'success'}"
        );
    }

    @Test
    public void testGetFolderContents()
    {
        testRequest(
            "get_folder_contents",
            "{ 'ShareId': '" + shares[ 0 ].Id + "', 'InternalName': '" + shareContent[ 0 ].InternalName + "' }",
            new Cookie[] {
                new Cookie( "primary_domain", primaryDomain ),
                new Cookie( "domain_token", domainToken ),
                new Cookie( "username", username )
            },
            "\\{'objects':\\[\\{'IsFile':.*?,'ShareId':'.*?','PublicName':'.*?','InternalName':'.*?'\\}.*\\}\\],'status':'success'}"
        );
    }

    @Test
    public void testCreateLinksToFiles()
    {
        testRequest(
            "create_links_to_files",
            "{\n" +
            "  'objects': [\n" +
            "    {\n" +
            "      'InternalName': '" + shareContent[ 0 ].InternalName + "',\n" +
            "      'ShareId': '" + shareContent[ 0 ].ShareId + "',\n" +
            "      'DaysToExpire': 10,\n" +
            "      'MaxUse': 5,\n" +
            "      'Message': 'hello world',\n" +
            "      'Password': '123456'\n" +
            "    },\n" +
            "    {\n" +
            "      'InternalName': '" + shareContent[ 0 ].InternalName + "',\n" +
            "      'ShareId': '" + shareContent[ 0 ].ShareId + "'\n" +
            "    }\n" +
            "  ]\n" +
            "}",
            new Cookie[] {
                new Cookie( "primary_domain", primaryDomain ),
                new Cookie( "domain_token", domainToken ),
                new Cookie( "username", username )
            },
            "\\{'objects':\\[\\{'MaxUse':5,'DaysToExpire':10,'ShareId':'" + shareContent[ 0 ].ShareId +
            "','Message':'hello world','InternalName':'" + shareContent[ 0 ].InternalName + "','Link':'https://" +
            primaryDomain +
            "/client/publiclink.aspx\\?id=.*?','Password':'123456'\\},\\{'MaxUse':'null','DaysToExpire':'null','ShareId':'" +
            shareContent[ 0 ].ShareId + "','Message':'null','InternalName':'" + shareContent[ 0 ].InternalName +
            "','Link':'https://" + primaryDomain +
            "/client/publiclink.aspx\\?id\\=.*?','Password':'null'\\}\\],'status':'success'\\}"
        );
    }

    @ParameterizedTest( name = "{0}" )
    @CsvSource( {
        "primary_domain",
        "domain_token",
        "username",
    } )
    public void testRequestsWithMissedAuthorizationCookies( final String cookieMissed )
    {
        final ArrayList<String> cookiesAvailable = new ArrayList<>()
        {{
            add( "primary_domain" );
            add( "domain_token" );
            add( "username" );
        }};
        cookiesAvailable.remove( cookieMissed );

        final Cookie[] cookiesReq = new Cookie[ 2 ];
        for( int i = 0; i < cookiesAvailable.size(); i++ ) {
            cookiesReq[ i ] = new Cookie( cookiesAvailable.get( i ), "any" );
        }

        testRequest(
            "any",
            cookiesReq,
            "\\{'message':'cookie <" + cookieMissed + "> missing','status':'error'\\}"
        );
    }

    @ParameterizedTest( name = "{0}" )
    @CsvSource( {
        "get_all_shares, '{ ShareId: any }'",
        "get_share_contents, '{ ShareId: any }'",
        "get_folder_contents, '{ ShareId: any, InternalName: any }'",
        "create_links_to_files, '{ objects: [ { ShareId: any, InternalName: any } ] }'",
    } )
    public void testRequestsWithInvalidPrimaryDomain( final String route, final String body )
    {
        testRequest(
            route,
            body,
            new Cookie[] {
                new Cookie( "primary_domain", "any" ),
                new Cookie( "domain_token", domainToken ),
                new Cookie( "username", username )
            },
            "\\{'message':'unknown host: clientgateway.any','status':'error'\\}"
        );
    }

    @ParameterizedTest( name = "{0}" )
    @CsvSource( {
        "get_all_shares, '{ ShareId: any }'",
        "get_share_contents, '{ ShareId: any }'",
        "get_folder_contents, '{ ShareId: any, InternalName: any }'",
        "create_links_to_files, '{ objects: [ { ShareId: any, InternalName: any } ] }'",
    } )
    public void testRequestsWithExpiredDomainToken( final String route, final String body )
    {
        testRequest(
            route,
            body,
            new Cookie[] {
                new Cookie( "primary_domain", primaryDomain ),
                new Cookie( "domain_token", "any" ),
                new Cookie( "username", username )
            },
            "\\{'message':'unauthorized','status':'error'\\}"
        );
    }

    @ParameterizedTest( name = "{0} :: <{1}>" )
    @CsvSource( {
        "authorize, password, '{ username: any }'",
        "authorize, username, '{ password: any }'",
        "get_share_contents, ShareId, '{}'",
        "get_folder_contents, ShareId, '{ InternalName: any }'",
        "get_folder_contents, InternalName, '{ ShareId: any }'",
        "create_links_to_files, ShareId, '{ objects: [ { InternalName: any } ] }'",
        "create_links_to_files, InternalName, '{ objects: [ { ShareId: any } ] }'",
    } )
    public void testRequestsWithMissedRequiredParameters( final String route, final String paramMissed,
                                                          final String body )
    {
        testRequest(
            route,
            body,
            new Cookie[] {
                new Cookie( "primary_domain", primaryDomain ),
                new Cookie( "domain_token", domainToken ),
                new Cookie( "username", username )
            },
            "\\{'message':'parameter <" + paramMissed + "> missing','status':'error'\\}"
        );
    }

    @ParameterizedTest( name = "{0}" )
    @CsvSource( {
        "get_share_contents, '{ ShareId: any }', 'No share could be found with the requested id.'",
        "get_folder_contents, '{ ShareId: any, InternalName: any }', 'No share could be found with the requested id.'",
        "create_links_to_files, '{ objects: [ { ShareId: any, InternalName: any } ] }', 'No share could be found with the requested id.'",
    } )
    public void testRequestsWithInvalidParameter( final String route, final String body, final String messageExpected )
    {
        testRequest(
            route,
            body,
            new Cookie[] {
                new Cookie( "primary_domain", primaryDomain ),
                new Cookie( "domain_token", domainToken ),
                new Cookie( "username", username )
            },
            "\\{'message':'" + messageExpected + "','status':'error'\\}"
        );
    }

    private void testRequest( final String route, final Cookie[] cookies, final String expectedMatch )
    {
        testRequest( route, "", cookies, expectedMatch );
    }

    private void testRequest( final String route, final String body, final String expectedMatch )
    {
        testRequest( route, body, null, expectedMatch );
    }

    private void testRequest( final String route, final String body, final Cookie[] cookies,
                              final String expectedMatch )
    {
        try {
            final HttpServletRequest request = mock( HttpServletRequest.class );
            final HttpServletResponse response = mock( HttpServletResponse.class );
            final ServletOutputStream output = mock( ServletOutputStream.class );
            final PrintWriter writer = new PrintWriter( new StringWriter() );

            when( request.getRequestURI() ).thenReturn( "/service/extension/rushfiles/" + route );
            when( response.getOutputStream() ).thenReturn( output );
            when( response.getWriter() ).thenReturn( writer );
            if( cookies != null ) when( request.getCookies() ).thenReturn( cookies );
            if( body != null ) when( request.getReader() ).thenReturn( new BufferedReader( new StringReader( body ) ) );

            new ExtensionHttpServlet().doPost( request, response );

            verify( output ).print( matches( expectedMatch.replaceAll( "'", "\"" ) ) );
        }
        catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }
}