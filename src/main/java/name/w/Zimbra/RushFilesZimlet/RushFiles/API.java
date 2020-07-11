package name.w.Zimbra.RushFilesZimlet.RushFiles;

import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class API
{
    private final String primaryDomain;
    private final String domainToken;
    private final String username;
    private final NetworkDriver network;

    protected API( final String username, final String password, final NetworkDriver network ) throws APIException
    {
        this.network = network;
        this.username = username;
        primaryDomain = getPrimaryDomain( username );
        final String deviceId = registerDevice( primaryDomain, username, password, "device", "os", 0 );
        domainToken = generateDomainToken( primaryDomain, username, password, deviceId, 0, 0 );
    }

    protected API( final String primaryDomain, final String domainToken, final String username, final NetworkDriver network )
    {
        this.network = network;
        this.primaryDomain = primaryDomain;
        this.domainToken = domainToken;
        this.username = username;
    }

    public String getPrimaryDomain()
    {
        return primaryDomain;
    }

    public String getDomainToken()
    {
        return domainToken;
    }

    public String getUsername()
    {
        return username;
    }

    private String getPrimaryDomain( final String username ) throws APIException
    {
        final String response = network.request(
            "https://global.rushfiles.com/getuserdomain.aspx?useremail=" + username,
            NetworkDriver.RequestMethod.GET,
            null,
            null
        );
        if( response.isEmpty() ) {
            throw new APIException( "Do not found primary domain for username " + username );
        }
        return response.replaceFirst( ".*,", "" );
    }

    private String registerDevice( final String primaryDomain, final String username, final String password,
                                   final String deviceName, final String deviceOs, final int deviceType )
        throws APIException
    {
        try {
            final String deviceId = buildUniqueIdByUsername( username );

            final var json = new JSONObject();
            json.put( "UserName", username );
            json.put( "Password", password );
            json.put( "DeviceName", deviceName );
            json.put( "DeviceOs", deviceOs );
            json.put( "DeviceType", deviceType );

            JSONObject jsonObject = new JSONObject( network.requestPUT(
                "https://clientgateway." + primaryDomain + "/api/devices/" + deviceId,
                null,
                json
            ) );
            final String message = jsonObject
                .get( "Message" )
                .toString();
            if( message.equals( "Ok." ) ) {
                return deviceId;
            }
            throw new APIException( message );
        }
        catch( JSONException e ) {
            throw new RuntimeException( e );
        }
    }

    private String generateDomainToken( final String primaryDomain, final String username, final String password,
                                        final String deviceId, final int longitude, final int latitude )
        throws APIException
    {
        try {
            final var json = new JSONObject();
            json.put( "UserName", username );
            json.put( "Password", password );
            json.put( "DeviceId", deviceId );
            json.put( "Longitude", longitude );
            json.put( "Latitude", latitude );

            final JSONObject jsonObject = new JSONObject( network.requestPOST(
                "https://clientgateway." + primaryDomain + "/api/domaintokens",
                null,
                json
            ) );
            final String message = jsonObject
                .get( "Message" )
                .toString();
            if( message.equals( "Ok." ) ) {
                return jsonObject
                    .getJSONObject( "Data" )
                    .getJSONArray( "DomainTokens" )
                    .getJSONObject( 0 )
                    .get( "DomainToken" )
                    .toString();
            }
            throw new APIException( "Unable to obtain domain token: " + message );
        }
        catch( JSONException e ) {
            throw new RuntimeException( e );
        }
    }

    public String createPublicLink( final String shareId, final String internalName, final int daysToExpire,
                                    final int maxUse, final String message )
        throws APIException
    {
        return createPublicLink( shareId, internalName, daysToExpire, maxUse, message, null );
    }

    public String createPublicLink( final String shareId, final String internalName, final Integer daysToExpire,
                                    final Integer maxUse, final String message, final String password )
        throws APIException
    {
        try {
            final var json = new JSONObject();
            json.put( "ShareId", shareId );
            json.put( "InternalName", internalName );
            if( daysToExpire != null ) {
                json.put( "DaysToExpire", daysToExpire );
            }
            if( maxUse != null ) {
                json.put( "MaxUse", maxUse );
            }
            if( message != null ) {
                json.put( "Message", message );
            }
            if( password != null ) {
                json.put( "EnablePassword", "true" );
                json.put( "Password", password );
            }

            // if requested creation of permanent link and such type of link already exists, return existing link
            final boolean doesLinkWillBePermanent = ( daysToExpire == null || daysToExpire <= 0 );
            if( doesLinkWillBePermanent ) {
                final PublicLink[] linksAlreadyCreated = getPublicLinksByFile( shareId, internalName );
                for( final PublicLink link: linksAlreadyCreated ) {
                    final boolean isCreatedLinkPermanent = ( ( int ) ( Math.log10( link.ExpirationTimeUtc ) + 1 ) ) == 19;
                    if( isCreatedLinkPermanent ) {
                        return "https://" + primaryDomain + "/client/publiclink.aspx?id=" + link.Id;
                    }
                }
            }

            final JSONObject result = new JSONObject( network.requestPOST(
                "https://clientgateway." + primaryDomain + "/api/publiclinks",
                domainToken,
                json
            ) );
            if( result
                .get( "Message" )
                .toString()
                .equals( "Public link has been created." ) ) {
                return result
                    .getJSONObject( "Data" )
                    .get( "FullLink" )
                    .toString();
            }
            throw new APIException( "Unable to create public link" );
        }
        catch( JSONException e ) {
            throw new RuntimeException( e );
        }
    }

    public VirtualFile[] getFolderContent( final String shareId, final String internalName )
        throws APIException
    {
        try {
            final JSONObject response = new JSONObject( network.requestGET(
                "https://clientgateway." + primaryDomain + "/api/shares/" + shareId + "/virtualfiles/" + internalName +
                "/children",
                domainToken
            ) );
            final String message = response.getString( "Message" );
            if( message.equals( "Ok." ) ) {
                return new Gson().fromJson( response
                    .getJSONArray( "Data" )
                    .toString(), VirtualFile[].class );
            }
            throw new APIException( message );
        }
        catch( JSONException e ) {
            throw new RuntimeException( e );
        }
    }

    public VirtualFile[] getShareContent( final String shareId )
        throws APIException
    {
        try {
            final JSONObject response = new JSONObject( network.requestGET(
                "https://clientgateway." + primaryDomain + "/api/shares/" + shareId +
                "/children?includeAssociations=false&includeDeleted=false",
                domainToken
            ) );
            final String message = response.getString( "Message" );
            if( message.equals( "Ok." ) ) {
                return new Gson().fromJson( response
                    .getJSONArray( "Data" )
                    .toString(), VirtualFile[].class );
            }
            throw new APIException( message );
        }
        catch( JSONException e ) {
            throw new RuntimeException( e );
        }
    }

    public Share[] getShares()
        throws APIException
    {
        try {
            final JSONObject response = new JSONObject( network.requestGET(
                "https://clientgateway." + primaryDomain + "/api/users/shares?userId=" + username +
                "&includeAssociations=false",
                domainToken
            ) );
            final String message = response.getString( "Message" );
            if( message.equals( "Ok." ) ) {
                return new Gson().fromJson( response
                    .getJSONArray( "Data" )
                    .toString(), Share[].class );
            }
            throw new APIException( message );
        }
        catch( JSONException e ) {
            throw new RuntimeException( e );
        }
    }

    public PublicLink[] getPublicLinksByFile( final String shareId, final String internalName )
        throws APIException
    {
        try {
            final JSONObject response = new JSONObject( network.requestGET(
                "https://clientgateway." + primaryDomain + "/api/shares/" + shareId + "/virtualfiles/" + internalName + "/publiclinks",
                domainToken
            ) );
            final String message = response.getString( "Message" );
            if( message.equals( "Ok." ) ) {
                return new Gson().fromJson( response
                    .getJSONArray( "Data" )
                    .toString(), PublicLink[].class );
            }
            throw new APIException( message );
        }
        catch( JSONException e ) {
            throw new RuntimeException( e );
        }
    }

    private String buildUniqueIdByUsername( final String username )
    {
        try {
            final MessageDigest digest = MessageDigest.getInstance( "SHA-256" );
            final byte[] hash = digest.digest( username.getBytes( StandardCharsets.UTF_8 ) );

            final StringBuilder hexString = new StringBuilder();
            for( byte b : hash ) {
                String hex = Integer.toHexString( 0xff & b );
                if( hex.length() == 1 ) {
                    hexString.append( '0' );
                }
                hexString.append( hex );
            }
            return hexString.toString();
        }
        catch( NoSuchAlgorithmException e ) {
            throw new RuntimeException( e );
        }
    }
}