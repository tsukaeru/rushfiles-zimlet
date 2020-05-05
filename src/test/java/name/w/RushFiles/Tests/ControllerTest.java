package name.w.RushFiles.Tests;

import name.w.RushFiles.API;
import name.w.RushFiles.APIException;
import name.w.RushFiles.Controller;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class ControllerTest
{
    protected static final String username = "hopster1222@gmail.com";
    protected static final String password = "L!-M/BBfol";

    protected static String primaryDomain;
    protected static String domainToken;
    protected static API.Share[] shares;
    protected static API.VirtualFile[] shareContent;

    @BeforeClass
    public static void beforeClass() throws APIException
    {
        primaryDomain = API.getPrimaryDomain( username );

        final String deviceId = API.registerDevice(
            primaryDomain,
            username,
            password,
            "device",
            "windows",
            0
        );
        domainToken = API.generateDomainToken( API.getPrimaryDomain( username ), username, password, deviceId, 0, 0 );

        shares = API.getShares( primaryDomain, domainToken, username );
        shareContent = API.getShareContent( primaryDomain, domainToken, shares[ 0 ].Id );
    }

    private static boolean isPrimaryDomain( final String subject )
    {
        return subject.equals( primaryDomain );
    }

    private static boolean isDomainToken( final String subject )
    {
        return subject.matches( "^.*Token.*?KeyId.*?Timestamp.*?Thumbprint.*$" );
    }

    private static boolean isUsername( final String subject )
    {
        return subject.equals( username );
    }

    private static boolean isPublicLink( final String subject )
    {
        return subject.matches( "https://.*?/client/publiclink.aspx\\?id=.*" );
    }

    @Test
    public void testAuthorization() throws JSONException
    {
        final JSONObject request = new JSONObject(
            "{ \"username\": \"" + username + "\", \"password\": \"" + password + "\" }"
        );
        final var controller = new Controller( "authorize", request );
        final JSONObject response = controller.getResponse();

        assertEquals( "success", response.getString( "status" ) );
        assertTrue( isPrimaryDomain( response.getString( "primary_domain" ) ) );
        assertTrue( isUsername( response.getString( "username" ) ) );
        assertTrue( isDomainToken( response.getString( "domain_token" ) ) );
    }

    @Test
    public void testAuthorizationParamPasswordMissed() throws JSONException
    {
        final JSONObject request = new JSONObject(
            "{ \"username\": \"" + username + "\" }"
        );
        final var controller = new Controller( "authorize", request );
        final JSONObject response = controller.getResponse();

        assertEquals( "error", response.getString( "status" ) );
        assertEquals( "parameter 'password' is missing", response.getString( "message" ) );
    }

    @Test
    public void testAuthorizationParamUsernameMissed() throws JSONException
    {
        final JSONObject request = new JSONObject(
            "{ \"password\": \"" + password + "\" }"
        );
        final var controller = new Controller( "authorize", request );
        final JSONObject response = controller.getResponse();

        assertEquals( "error", response.getString( "status" ) );
        assertEquals( "parameter 'username' is missing", response.getString( "message" ) );
    }

    @Test
    public void testAuthorizationFail() throws JSONException
    {
        final JSONObject request = new JSONObject(
            "{ \"username\": \"username@jopa.com\", \"password\": \"vsempizdec\" }"
        );
        final var controller = new Controller( "authorize", request );
        final JSONObject response = controller.getResponse();

        assertEquals( "error", response.getString( "status" ) );
        assertTrue( response.getString( "message" ).contains( "Do not found primary domain for" ) );
    }

    // TODO реакция на отсутствие полей для авторизации каждой из трех в каждой команде

    @Test
    public void testGetAllShares() throws JSONException
    {
        final var controller = new Controller(
            "get_all_shares",
            null,
            primaryDomain,
            domainToken,
            username
        );
        final JSONObject response = controller.getResponse();

        assertEquals( "success", response.get( "status" ) );
        assertNotNull( response.getJSONArray( "objects" ) );
        assertNotNull( response.getJSONArray( "objects" ).getJSONObject( 0 ) );
    }

    @Test
    public void testGetAllSharesUnauthorized() throws JSONException
    {
        final var controller = new Controller(
            "get_all_shares",
            null,
            primaryDomain,
            domainToken + "123",
            username
        );
        final JSONObject response = controller.getResponse();

        assertEquals( "error", response.getString( "status" ) );
        assertEquals( "unauthorized", response.getString( "message" ) );
    }

    @Test
    public void testGetShareContents() throws JSONException
    {
        final JSONObject request = new JSONObject().put( "ShareId", shares[ 0 ].Id );
        final var controller = new Controller( "get_share_contents", request, primaryDomain, domainToken, username );
        final JSONObject response = controller.getResponse();

        assertEquals( "success", response.get( "status" ) );
        assertNotNull( response.getJSONArray( "objects" ).getJSONObject( 0 ) );
    }

    @Test
    public void testGetShareContentsParamShareIdMissed() throws JSONException
    {
        final var controller = new Controller( "get_share_contents", new JSONObject(), primaryDomain, domainToken, username );
        final JSONObject response = controller.getResponse();

        assertEquals( "error", response.getString( "status" ) );
        assertEquals( "parameter 'ShareId' is missing", response.getString( "message" ) );
    }

    @Test
    public void testGetShareContentsInvalidRequest() throws JSONException
    {
        final JSONObject request = new JSONObject().put( "ShareId", "123" );
        final var controller = new Controller( "get_share_contents", request, primaryDomain, domainToken, username );
        final JSONObject response = controller.getResponse();

        assertEquals( "error", response.getString( "status" ) );
        assertTrue( response.getString( "message" ).contains( "No share could be found" ) );
    }

    @Test
    public void testGetFolderContents() throws JSONException
    {
        final API.VirtualFile file = shareContent[ 0 ];

        final JSONObject request = new JSONObject()
            .put( "ShareId", file.ShareId )
            .put( "InternalName", file.InternalName );
        final var controller = new Controller( "get_folder_contents", request, primaryDomain, domainToken, username );
        final JSONObject response = controller.getResponse();

        assertEquals( "success", response.get( "status" ) );
        assertNotNull( controller.getResponse().getJSONArray( "objects" ).getJSONObject( 0 ) );
    }

    @Test
    public void testGetShareContentUnauthorized() throws JSONException
    {
        final JSONObject request = new JSONObject().put( "ShareId", shares[ 0 ].Id );
        final var controller = new Controller( "get_share_contents", request,
            primaryDomain, domainToken + "123", username );
        final JSONObject response = controller.getResponse();

        assertEquals( "error", response.getString( "status" ) );
        assertEquals( "unauthorized", response.getString( "message" ) );
    }

    @Test
    public void testGetFolderContentsParamInternalNameMissed() throws JSONException
    {
        final API.VirtualFile file = shareContent[ 0 ];

        JSONObject request = new JSONObject()
            .put( "ShareId", file.ShareId );
        var controller = new Controller( "get_folder_contents", request, primaryDomain, domainToken, username );
        JSONObject response = controller.getResponse();
        assertEquals( "error", response.getString( "status" ) );
        assertEquals( "parameter 'InternalName' is missing", response.getString( "message" ) );
    }

    @Test
    public void testGetFolderContentsParamShareIdMissed() throws JSONException
    {
        final API.VirtualFile file = shareContent[ 0 ];

        JSONObject request = new JSONObject()
            .put( "InternalName", file.ShareId );
        Controller controller = new Controller( "get_folder_contents", request, primaryDomain, domainToken, username );
        JSONObject response = controller.getResponse();
        assertEquals( "error", response.getString( "status" ) );
        assertEquals( "parameter 'ShareId' is missing", response.getString( "message" ) );
    }

    @Test
    public void testGetFolderContentsInvalidRequest() throws JSONException
    {
        final JSONObject request = new JSONObject()
            .put( "ShareId", "123" )
            .put( "InternalName", "123" );
        final var controller = new Controller( "get_folder_contents", request, primaryDomain, domainToken, username );
        final JSONObject response = controller.getResponse();

        assertEquals( "error", response.getString( "status" ) );
        assertTrue( response.getString( "message" ).contains( "No share could be found" ) );
    }

    @Test
    public void testGetFolderContentsUnauthorized() throws JSONException
    {
        final API.VirtualFile file = shareContent[ 0 ];

        final JSONObject request = new JSONObject()
            .put( "ShareId", file.ShareId )
            .put( "InternalName", file.InternalName );
        final var controller = new Controller( "get_folder_contents", request,
            primaryDomain, domainToken + "123", username );
        final JSONObject response = controller.getResponse();

        assertEquals( "error", response.getString( "status" ) );
        assertEquals( "unauthorized", response.getString( "message" ) );
    }

    @Test
    public void testCreateLinksToFiles() throws JSONException
    {
        final API.VirtualFile file = shareContent[ 0 ];

        final ArrayList<JSONObject> sets = new ArrayList<>();
        sets.add( new JSONObject()
            .put( "ShareId", file.ShareId )
            .put( "InternalName", file.InternalName )
            .put( "DaysToExpire", 10 )
            .put( "MaxUse", 5 )
            .put( "Message", "helloworld" )
            .put( "Password", "123456" )
        );
        sets.add( new JSONObject()
            .put( "ShareId", file.ShareId )
            .put( "InternalName", file.InternalName )
            .put( "DaysToExpire", 20 )
            .put( "MaxUse", 10 )
            .put( "Message", "fuckoff" )
            .put( "Password", "qwerty" )
        );
        sets.add( new JSONObject()
            .put( "ShareId", file.ShareId )
            .put( "InternalName", file.InternalName )
        );

        final JSONArray linksOptions = new JSONArray();
        for( final JSONObject set : sets ) {
            linksOptions.put( set );
        }

        final JSONObject request = new JSONObject();
        request.put( "objects", linksOptions );

        final var controller = new Controller( "create_links_to_files", request, primaryDomain, domainToken, username );
        final JSONObject response = controller.getResponse();

        assertEquals( "success", response.get( "status" ) );
        for( int objectIdx = 0; objectIdx < response.getJSONArray( "objects" ).length(); objectIdx++ ) {
            final JSONObject objectOriginal = sets.get( objectIdx );
            final JSONObject objectResult = response.getJSONArray( "objects" ).getJSONObject( objectIdx );

            assertEquals( objectOriginal.get( "ShareId" ), objectResult.get( "ShareId" ) );
            assertEquals( objectOriginal.get( "InternalName" ), objectResult.get( "InternalName" ) );
            assertEquals(
                ( objectOriginal.has( "DaysToExpire" ) ) ? objectOriginal.get( "DaysToExpire" ) : "null",
                objectResult.get( "DaysToExpire" )
            );
            assertEquals(
                ( objectOriginal.has( "MaxUse" ) ) ? objectOriginal.get( "MaxUse" ) : "null",
                objectResult.get( "MaxUse" )
            );
            assertEquals(
                ( objectOriginal.has( "Message" ) ) ? objectOriginal.get( "Message" ) : "null",
                objectResult.get( "Message" )
            );
            assertEquals(
                ( objectOriginal.has( "Password" ) ) ? objectOriginal.get( "Password" ) : "null",
                objectResult.get( "Password" )
            );
            assertTrue( isPublicLink( objectResult.getString( "Link" ) ) );
        }
    }

    @Test
    public void testCreateLinksToFilesParamShareIdMissed() throws JSONException
    {
        final API.VirtualFile file = shareContent[ 0 ];

        final ArrayList<JSONObject> objects = new ArrayList<>();
        objects.add( new JSONObject().put( "InternalName", file.ShareId ) );
        final JSONObject request = new JSONObject();
        request.put( "objects", objects );

        var controller = new Controller( "create_links_to_files", request, primaryDomain, domainToken, username );
        final JSONObject response = controller.getResponse();

        assertEquals( "error", response.getString( "status" ) );
        assertEquals( "parameter 'ShareId' is missing", response.getString( "message" ) );
    }

    @Test
    public void testCreateLinksToFilesParamInternalNameMissed() throws JSONException
    {
        final API.VirtualFile file = shareContent[ 0 ];

        final ArrayList<JSONObject> objects = new ArrayList<>();
        objects.add( new JSONObject().put( "ShareId", file.ShareId ) );
        final JSONObject request = new JSONObject();
        request.put( "objects", objects );

        final var controller = new Controller( "create_links_to_files", request, primaryDomain, domainToken, username );
        final JSONObject response = controller.getResponse();

        assertEquals( "error", response.getString( "status" ) );
        assertEquals( "parameter 'InternalName' is missing", response.getString( "message" ) );
    }

    @Test
    public void testCreateLinksToFilesInvalidRequest() throws JSONException
    {
        final JSONArray linksOptions = new JSONArray();

        final JSONObject linkThreeRequest = new JSONObject()
            .put( "ShareId", "123" )
            .put( "InternalName", "123" );
        linksOptions.put( linkThreeRequest );

        final JSONObject request = new JSONObject();
        request.put( "objects", linksOptions );

        final var controller = new Controller( "links_to_files", request, primaryDomain, domainToken, username );
        final JSONObject response = controller.getResponse();

        assertEquals( "error", response.get( "status" ) );
    }

    @Test
    public void testCreateLinkToFilesUnauthorized() throws JSONException
    {
        final API.VirtualFile file = shareContent[ 0 ];

        final ArrayList<JSONObject> sets = new ArrayList<>();
        sets.add( new JSONObject()
            .put( "ShareId", file.ShareId )
            .put( "InternalName", file.InternalName )
        );

        final JSONArray linksOptions = new JSONArray();
        for( final JSONObject set : sets ) {
            linksOptions.put( set );
        }

        final JSONObject request = new JSONObject();
        request.put( "objects", linksOptions );

        final var controller = new Controller( "create_links_to_files", request,
            primaryDomain, domainToken + "123", username );
        final JSONObject response = controller.getResponse();

        assertEquals( "error", response.getString( "status" ) );
        assertEquals( "unauthorized", response.getString( "message" ) );
    }
}