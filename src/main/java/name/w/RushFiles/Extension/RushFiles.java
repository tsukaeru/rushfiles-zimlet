package name.w.RushFiles.Extension;

import com.zimbra.cs.extension.ExtensionHttpHandler;
import name.w.RushFiles.API;
import name.w.RushFiles.APIException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

public class RushFiles extends ExtensionHttpHandler
{
    private JSONObject request;
    private String primaryDomain;
    private String domainToken;
    private String username;

    public void doPost( final HttpServletRequest req, final HttpServletResponse resp ) throws IOException
    {
        resp.setHeader( "Content-Type", "text/json;charset=UTF-8" );

        JSONObject response;
        try {
            final String route = req.getRequestURI().replaceFirst( "/service/extension" + getPath() + "/", "" );
            final String body = req.getReader().lines().collect( Collectors.joining( System.lineSeparator() ) );

            final JSONObject request = ( isJSONValid( body ) ? new JSONObject( body ) : null );
            final Cookie primaryDomainCookie = getCookieByName( "primary_domain", req.getCookies() );
            final Cookie domainTokenCookie = getCookieByName( "domain_token", req.getCookies() );
            final Cookie usernameCookie = getCookieByName( "username", req.getCookies() );

            this.request = request;
            this.primaryDomain = ( primaryDomainCookie == null ) ? null : primaryDomainCookie.getValue();
            this.domainToken = ( domainTokenCookie == null ) ? null : domainTokenCookie.getValue();
            this.username = ( usernameCookie == null ) ? null : usernameCookie.getValue();

            if( route.equals( "authorize" ) ) {
                response = authorize();
            }
            else if( primaryDomain == null || primaryDomain.isEmpty() ) {
                throw new RushFilesException( "cookie <primary_domain> missing" );
            }
            else if( domainToken == null || domainToken.isEmpty() ) {
                throw new RushFilesException( "cookie <domain_token> missing" );
            }
            else if( username == null || username.isEmpty() ) {
                throw new RushFilesException( "cookie <username> missing" );
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
                throw new RushFilesException( "unknown route: " + route );
            }

        }
        catch( JSONException e ) {
            throw new RuntimeException( e );
        }
        catch( RushFilesException | APIException e ) {
            try {
                response = new JSONObject().put( "status", "error" ).put( "message", e.getMessage() );
            }
            catch( JSONException j ) {
                throw new RuntimeException( j );
            }
        }
        resp.getOutputStream().print( response.toString() );
    }

    public String getPath()
    {
        return "/rushfiles";
    }

    private boolean isJSONValid( final String json )
    {
        try {
            new JSONObject( json );
        }
        catch( JSONException ex ) {
            try {
                new JSONArray( json );
            }
            catch( JSONException ex1 ) {
                return false;
            }
        }
        return true;
    }

    private Cookie getCookieByName( final String name, final Cookie[] cookies )
    {
        if( cookies == null ) {
            return null;
        }
        for( final Cookie cookie : cookies ) {
            if( name.equals( cookie.getName() ) ) {
                return cookie;
            }
        }
        return null;
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
            if( request == null || ! request.has( paramRequired ) ) {
                throw new APIException( "parameter <" + paramRequired + "> missing" );
            }
        }
    }
}
