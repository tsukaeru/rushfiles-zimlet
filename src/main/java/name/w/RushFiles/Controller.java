package name.w.RushFiles;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Controller
{
    private final JSONObject request;
    private final String primaryDomain;
    private final String domainToken;
    private final String username;
    private final JSONObject response;

    public Controller( final String route, final JSONObject request,
                       final String primaryDomain, final String domainToken, final String username )
    {
        this.request = request;
        this.primaryDomain = primaryDomain;
        this.domainToken = domainToken;
        this.username = username;
        JSONObject response;

        try {
            if( route.equals( "authorize" ) ) {
                response = authorize();
            }
            else if( primaryDomain == null || primaryDomain.isEmpty() ) {
                throw new ControllerException( "primary domain is empty" );
            }
            else if( domainToken == null || domainToken.isEmpty() ) {
                throw new ControllerException( "domain token is empty" );
            }
            else if( username == null || username.isEmpty() ) {
                throw new ControllerException( "username is empty" );
            }
            else if( route.equals( "get_all_shares" ) ) {
                response = new JSONObject().put( "status", "success" ).put( "objects", getAllShares() );
            }
            else if( route.equals( "get_share_contents" ) ) {
                response = new JSONObject().put( "status", "success" ).put( "objects", getShareContents() );
            }
            else if( route.equals( "get_folder_contents" ) ) {
                response = new JSONObject().put( "status", "success" ).put( "objects", getFolderContents() );
            }
            else if( route.equals( "create_links_to_files" ) ) {
                response = new JSONObject().put( "status", "success" ).put( "objects", createLinksToFiles() );
            }
            else {
                throw new ControllerException( "unknown route: " + route );
            }
        }
        catch( ControllerException|APIException e ) {
            try {
                response = new JSONObject().put( "status", "error" ).put( "message", e.getMessage() );
            }
            catch( JSONException j ) {
                throw new RuntimeException( j );
            }
        }
        catch( JSONException e ) {
            throw new RuntimeException( e );
        }
        this.response = response;
    }

    public Controller( final String target, final JSONObject request )
    {
        this( target, request, null, null, null );
    }

    private JSONObject authorize() throws JSONException, APIException
    {
        checkParamsPresence( request, "username", "password" );

        final String username = request.getString( "username" );
        final String password = request.getString( "password" );
        final String primaryDomain = API.getPrimaryDomain( username );
        final String deviceId = API.registerDevice( primaryDomain, username, password, "device", "os", 0 );
        final String domainToken = API.generateDomainToken( primaryDomain, username, password, deviceId, 0, 0 );
        return new JSONObject()
            .put( "status", "success" )
            .put( "primary_domain", primaryDomain )
            .put( "domain_token", domainToken )
            .put( "username", username )
            ;
    }

    private JSONArray getAllShares() throws APIException
    {
        final JSONArray shares = new JSONArray();
        for( final API.Share share : API.getShares( primaryDomain, domainToken, username ) ) {
            shares.put( share.toJson() );
        }
        return shares;
    }

    private JSONArray getShareContents() throws APIException, JSONException
    {
        checkParamsPresence( request, "ShareId" );

        final var result = new JSONArray();
        final API.VirtualFile[] files = API.getShareContent(
            primaryDomain,
            domainToken,
            request.getString( "ShareId" )
        );
        for( final API.VirtualFile file : files ) {
            result.put( file.toJson() );
        }
        return result;
    }

    private JSONArray getFolderContents() throws JSONException, APIException
    {
        checkParamsPresence( request, "ShareId", "InternalName" );

        final var result = new JSONArray();
        final API.VirtualFile[] files = API.getFolderContent(
            primaryDomain,
            request.getString( "ShareId" ),
            request.getString( "InternalName" ),
            domainToken
        );
        for( final API.VirtualFile file : files ) {
            result.put( file.toJson() );
        }
        return result;
    }

    private JSONArray createLinksToFiles() throws JSONException, APIException
    {
        checkParamsPresence( request, "objects" );

        final JSONArray result = new JSONArray();
        final JSONArray files = request.getJSONArray( "objects" );
        for( int fileIdx = 0; fileIdx < files.length(); fileIdx++ ) {
            final JSONObject file = files.getJSONObject( fileIdx );

            checkParamsPresence( file, "ShareId", "InternalName" );

            final String shareId = file.getString( "ShareId" );
            final String internalName = file.getString( "InternalName" );
            final Integer daysToExpire = ( file.has( "DaysToExpire" ) ) ? file.getInt( "DaysToExpire" ) : null;
            final Integer maxUse = ( file.has( "MaxUse" ) ) ? file.getInt( "MaxUse" ) : null;
            final String message = ( file.has( "Message" ) ) ? file.getString( "Message" ) : null;
            final String password = ( file.has( "Password" ) ) ? file.getString( "Password" ) : null;

            final JSONObject linkCreated = new JSONObject();
            linkCreated.put( "Link", API.createPublicLink(
                primaryDomain,
                domainToken,
                shareId,
                internalName,
                daysToExpire,
                maxUse,
                message,
                password
            ) );
            linkCreated
                .put( "ShareId", shareId )
                .put( "InternalName", internalName )
                .put( "DaysToExpire", ( daysToExpire == null ) ? "null" : daysToExpire )
                .put( "MaxUse", ( maxUse == null ) ? "null" : maxUse )
                .put( "Message", ( message == null ) ? "null" : message )
                .put( "Password", ( password == null ) ? "null" : password )
            ;
            result.put( linkCreated );
        }
        return result;
    }

    private void checkParamsPresence( final JSONObject request, final String... params ) throws APIException
    {
        for( final String paramRequired : params ) {
            if( ! request.has( paramRequired ) ) {
                throw new APIException( "parameter '" + paramRequired + "' is missing" );
            }
        }
    }

    public JSONObject getResponse()
    {
        return response;
    }
}
