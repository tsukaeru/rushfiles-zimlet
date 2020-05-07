package name.w.Zimbra.RushFilesZimlet;

import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class RushFilesAPI
{
    public static String getPrimaryDomain( final String username ) throws RushFilesAPIException
    {
        final String response = request(
            "https://global.rushfiles.com/getuserdomain.aspx?useremail=" + username,
            RequestMethod.GET,
            null,
            null
        );
        if( response.isEmpty() ) {
            throw new RushFilesAPIException( "Do not found primary domain for username " + username );
        }
        return response.replaceFirst( ".*,", "" );
    }

    public static String registerDevice( final String primaryDomain, final String username, final String password,
                                         final String deviceName, final String deviceOs, final int deviceType )
        throws RushFilesAPIException
    {
        try {
            final String deviceId = buildUniqueIdByUsername( username );

            final var json = new JSONObject();
            json.put( "UserName", username );
            json.put( "Password", password );
            json.put( "DeviceName", deviceName );
            json.put( "DeviceOs", deviceOs );
            json.put( "DeviceType", deviceType );

            JSONObject jsonObject = new JSONObject( requestPUT(
                "https://clientgateway." + primaryDomain + "/api/devices/" + deviceId,
                json
            ) );
            final String message = jsonObject
                .get( "Message" )
                .toString();
            if( message.equals( "Ok." ) ) {
                return deviceId;
            }
            throw new RushFilesAPIException( message );
        }
        catch( JSONException e ) {
            throw new RuntimeException( e );
        }
    }

    public static String generateDomainToken( final String primaryDomain, final String username, final String password,
                                              final String deviceId, final int longitude, final int latitude )
        throws RushFilesAPIException
    {
        try {
            final var json = new JSONObject();
            json.put( "UserName", username );
            json.put( "Password", password );
            json.put( "DeviceId", deviceId );
            json.put( "Longitude", longitude );
            json.put( "Latitude", latitude );

            final JSONObject jsonObject = new JSONObject( requestPOST(
                "https://clientgateway." + primaryDomain + "/api/domaintokens",
                json,
                null
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
            throw new RushFilesAPIException( "Unable to obtain domain token: " + message );
        }
        catch( JSONException e ) {
            throw new RuntimeException( e );
        }
    }

    public static String createPublicLink( final String primaryDomain, final String domainToken, final String shareId,
                                           final String internalName, final int daysToExpire, final int maxUse,
                                           final String message )
        throws RushFilesAPIException
    {
        return createPublicLink( primaryDomain, domainToken, shareId, internalName,
            daysToExpire, maxUse, message, null
        );
    }

    public static String createPublicLink( final String primaryDomain, final String domainToken, final String shareId,
                                           final String internalName, final Integer daysToExpire, final Integer maxUse,
                                           final String message, final String password )
        throws RushFilesAPIException
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

            final JSONObject result = new JSONObject( requestPOST(
                "https://clientgateway." + primaryDomain + "/api/publiclinks",
                json,
                domainToken
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
            throw new RushFilesAPIException( "Unable to create public link" );
        }
        catch( JSONException e ) {
            throw new RuntimeException( e );
        }
    }

    public static VirtualFile[] getFolderContent( final String primaryDomain, final String shareId,
                                                  final String internalName, final String domainToken )
        throws RushFilesAPIException
    {
        try {
            final JSONObject response = new JSONObject( requestGET(
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
            throw new RushFilesAPIException( message );
        }
        catch( JSONException e ) {
            throw new RuntimeException( e );
        }
    }

    public static VirtualFile[] getShareContent( final String primaryDomain, final String domainToken,
                                                 final String shareId )
        throws RushFilesAPIException
    {
        try {
            final JSONObject response = new JSONObject( requestGET(
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
            throw new RushFilesAPIException( message );
        }
        catch( JSONException e ) {
            throw new RuntimeException( e );
        }
    }

    public static Share[] getShares( final String primaryDomain, final String domainToken, final String username )
        throws RushFilesAPIException
    {
        try {
            final JSONObject response = new JSONObject( requestGET(
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
            throw new RushFilesAPIException( message );
        }
        catch( JSONException e ) {
            throw new RuntimeException( e );
        }
    }

    private static String buildUniqueIdByUsername( final String username )
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

    private static String requestGET( final String url, final String domainToken ) throws RushFilesAPIException
    {
        return request( url, RequestMethod.GET, null, domainToken );
    }

    private static String requestPOST( final String url, final JSONObject json, final String domainToken )
        throws RushFilesAPIException
    {
        return request( url, RequestMethod.POST, json, domainToken );
    }

    private static String requestPUT( final String url, final JSONObject json ) throws RushFilesAPIException
    {
        return request( url, RequestMethod.PUT, json, null );
    }

    private static String request( final String url, final RequestMethod method, final JSONObject json,
                                   final String domainToken )
        throws RushFilesAPIException
    {
        try {
            final var con = ( HttpURLConnection ) new URL( url ).openConnection();
            con.setDoOutput( true );

            con.setRequestMethod( method.name() );
            con.setRequestProperty( "Content-Type", "application/json" );
            if( domainToken != null ) {
                con.setRequestProperty( "Authorization", "DomainToken " + domainToken );
            }

            if( json != null ) {
                final OutputStream os = con.getOutputStream();
                os.write( json
                    .toString()
                    .getBytes( StandardCharsets.UTF_8 ) );
                os.close();
            }

            final InputStream in;
            if( con.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED ) {
                throw new RushFilesAPIException( "unauthorized" );
            }
            else if( con.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST ) {
                throw new RushFilesAPIException( "bad request" );
            }
            else if( con.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND ||
                     con.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN ) {
                in = new BufferedInputStream( con.getErrorStream() );
            }
            else {
                in = new BufferedInputStream( con.getInputStream() );
            }
            final BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );

            final StringBuilder result = new StringBuilder();
            String line;
            while( ( line = reader.readLine() ) != null ) {
                result.append( line );
            }

            return String.valueOf( result );
        }
        catch( UnknownHostException e ) {
            throw new RushFilesAPIException( "unknown host: " + e.getMessage() );
        }
        catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private enum RequestMethod
    {
        GET, POST, PUT
    }

    public static class Share
    {
        public final String Id;
        public final String CompanyId;
        public final String Name;

        private Share( final String aId, final String aCompanyId, final String aName )
        {
            Id = aId;
            CompanyId = aCompanyId;
            Name = aName;
        }

        public JSONObject toJson()
        {
            try {
                return new JSONObject()
                    .put( "Id", Id )
                    .put( "CompanyId", CompanyId )
                    .put( "Name", Name );
            }
            catch( JSONException e ) {
                throw new RuntimeException( e );
            }
        }
    }

    public static class VirtualFile
    {
        public final boolean IsFile;
        public final String InternalName;
        public final String PublicName;
        public final String ShareId;

        private VirtualFile( final boolean aIsFile, final String aInternalName, final String aPublicName,
                             final String aShareId )
        {
            IsFile = aIsFile;
            InternalName = aInternalName;
            PublicName = aPublicName;
            ShareId = aShareId;
        }

        public JSONObject toJson()
        {
            try {
                return new JSONObject()
                    .put( "IsFile", IsFile )
                    .put( "InternalName", InternalName )
                    .put( "PublicName", PublicName )
                    .put( "ShareId", ShareId );
            }
            catch( JSONException e ) {
                throw new RuntimeException( e );
            }
        }
    }
}