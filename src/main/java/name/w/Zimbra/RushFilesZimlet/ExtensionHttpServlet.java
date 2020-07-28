package name.w.Zimbra.RushFilesZimlet;

import com.zimbra.cs.extension.ExtensionHttpHandler;
import name.w.Zimbra.RushFilesZimlet.RushFiles.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ExtensionHttpServlet extends ExtensionHttpHandler
{
    private HttpServletResponse response;
    private RequestData data;
    private API api;

    public String getPath()
    {
        return "/rushfiles";
    }

    public void doPost( final HttpServletRequest request, final HttpServletResponse response ) throws IOException
    {
        this.response = response;
        this.response.setHeader( "Content-Type", "text/json;charset=UTF-8" );

        try {
            data = new RequestData( request );

            // standard auth
            if( data.route.equals( "authorize" ) ) {
                authorize();
                return;
            }

            // on-the-fly auth
            if( data.requestUsername != null && data.requestPassword != null ) {
                api = new Authenticator().unauthorized( data.requestUsername, data.requestPassword );
            }
            // authed
            else {
                if( data.cookiePrimaryDomain == null || data.cookiePrimaryDomain.isEmpty() ) {
                    throw new ExtensionHttpServletException( "cookie <primary_domain> missing" );
                }
                else if( data.cookieDomainToken == null || data.cookieDomainToken.isEmpty() ) {
                    throw new ExtensionHttpServletException( "cookie <domain_token> missing" );
                }
                else if( data.cookieUsername == null || data.cookieUsername.isEmpty() ) {
                    throw new ExtensionHttpServletException( "cookie <username> missing" );
                }

                api = new Authenticator().authorized(
                    data.cookiePrimaryDomain,
                    data.cookieDomainToken,
                    data.cookieUsername
                );
            }

            final String route = data.route;
            switch( route ) {
                case "get_all_shares":
                    getAllShares(); break;
                case "get_share_contents":
                    getShareContents(); break;
                case "get_folder_contents":
                    getFolderContents(); break;
                case "create_links_to_files":
                    createLinksToFiles(); break;
                default:
                    throw new ExtensionHttpServletException( "unknown route: " + route );
            }
        }
        catch( JSONException e ) {
            throw new RuntimeException( e );
        }
        catch( ExtensionHttpServletException | APIException e ) {
            try {
                setResponseError( e.getMessage() );
            }
            catch( JSONException j ) {
                throw new RuntimeException( j );
            }
        }
    }

    private void authorize() throws ExtensionHttpServletException, APIException, IOException
    {
        if( data.requestUsername == null ) {
            throw new ExtensionHttpServletException( "parameter <username> missing" );
        }
        if( data.requestPassword == null ) {
            throw new ExtensionHttpServletException( "parameter <password> missing" );
        }
        final API api = new Authenticator().unauthorized( data.requestUsername, data.requestPassword );
        setResponseSuccess(
            new JSONObject()
                .put( "primary_domain", api.getPrimaryDomain() )
                .put( "domain_token", api.getDomainToken() )
                .put( "username", api.getUsername() )
        );
    }

    private void getAllShares() throws APIException, IOException
    {
        final JSONArray shares = new JSONArray();
        for( final Share share : api.getShares() ) {
            shares.put( share.toJson() );
        }
        setResponseSuccess( shares );
    }

    private void getShareContents() throws ExtensionHttpServletException, APIException, JSONException, IOException
    {
        if( data.requestShareId == null ) {
            throw new ExtensionHttpServletException( "parameter <ShareId> missing" );
        }

        final var result = new JSONArray();
        final VirtualFile[] files = api.getShareContent( data.requestShareId );
        for( final VirtualFile file : files ) {
            result.put( file.toJson() );
        }

        setResponseSuccess( result );
    }

    private void getFolderContents() throws JSONException, ExtensionHttpServletException, APIException, IOException
    {
        if( data.requestShareId == null ) {
            throw new ExtensionHttpServletException( "parameter <ShareId> missing" );
        }
        if( data.requestInternalName == null ) {
            throw new ExtensionHttpServletException( "parameter <InternalName> missing" );
        }

        final var result = new JSONArray();
        final VirtualFile[] files = api.getFolderContent(
            data.requestShareId,
            data.requestInternalName
        );
        for( final VirtualFile file : files ) {
            result.put( file.toJson() );
        }

        setResponseSuccess( result );
    }

    private void createLinksToFiles() throws JSONException, ExtensionHttpServletException, APIException, IOException
    {
        if( data.requestObjects == null ) {
            throw new ExtensionHttpServletException( "parameter <InternalName> missing" );
        }

        final JSONArray result = new JSONArray();
        final JSONArray files = data.requestObjects;
        for( int fileIdx = 0; fileIdx < files.length(); fileIdx++ ) {
            final JSONObject file = files.getJSONObject( fileIdx );
            if( ! file.has( "ShareId" ) ) {
                throw new ExtensionHttpServletException( "parameter <ShareId> missing" );
            }
            if( ! file.has( "InternalName" ) ) {
                throw new ExtensionHttpServletException( "parameter <InternalName> missing" );
            }

            final String shareId = file.getString( "ShareId" );
            final String internalName = file.getString( "InternalName" );
            final Integer daysToExpire = ( file.has( "DaysToExpire" ) ) ? file.getInt( "DaysToExpire" ) : null;
            final Integer maxUse = ( file.has( "MaxUse" ) ) ? file.getInt( "MaxUse" ) : null;
            final String message = ( file.has( "Message" ) ) ? file.getString( "Message" ) : null;
            final String password = ( file.has( "Password" ) ) ? file.getString( "Password" ) : null;

            final JSONObject linkCreated = new JSONObject();
            linkCreated.put( "Link", api.createPublicLink(
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

        setResponseSuccess( result );
    }

    private void setResponseSuccess( final JSONObject response ) throws IOException
    {
        setResponse( response.put( "status", "success" ) );
    }

    private void setResponseSuccess( final JSONArray response ) throws IOException
    {
        setResponseSuccess( new JSONObject().put( "objects", response ) );
    }

    private void setResponseError( final String message ) throws IOException
    {
        setResponse( new JSONObject().put( "status", "error" ).put( "message", message ) );
    }

    private void setResponse( final JSONObject response ) throws IOException
    {
        this.response.getOutputStream().print( response.toString() );
    }
}
