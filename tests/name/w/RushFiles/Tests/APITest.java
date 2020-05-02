package name.w.RushFiles.Tests;

import name.w.RushFiles.API;
import name.w.RushFiles.APIException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class APITest extends Data
{
    @Test
    public void testPrimaryDomainGetting() throws APIException
    {
        assertTrue( isPrimaryDomain( API.getPrimaryDomain( username ) ) );
    }

    @Test
    public void testDeviceRegistration() throws APIException
    {
        assertEquals(
            64,
            API.registerDevice( API.getPrimaryDomain( username ), username, password, "device", "windows", 0 ).length()
        );
    }

    @Test
    public void testDomainTokenGeneration() throws APIException
    {
        final String deviceId = API.registerDevice(
            API.getPrimaryDomain( username ),
            username,
            password,
            "device",
            "windows",
            0
        );
        API.generateDomainToken( API.getPrimaryDomain( username ), username, password, deviceId, 0, 0 );
    }

    @Test
    public void testPublicLinkCreation() throws APIException
    {
        final String linkWithPassword = API.createPublicLink(
            primaryDomain,
            domainToken,
            shareContent[ 0 ].ShareId,
            shareContent[ 0 ].InternalName,
            0,
            0,
            "hello",
            "123456"
        );
        final String linkWithoutPassword = API.createPublicLink(
            primaryDomain,
            domainToken,
            shareContent[ 0 ].ShareId,
            shareContent[ 0 ].InternalName,
            0,
            0,
            "hello"
        );

        assertTrue( isPublicLink( linkWithPassword ) );
        assertTrue( isPublicLink( linkWithoutPassword ) );
    }

    @Test
    public void testFolderContentGetting() throws APIException
    {
        final API.VirtualFile[] folderContent = API.getFolderContent(
            primaryDomain,
            shareContent[ 0 ].ShareId,
            shareContent[ 0 ].InternalName,
            domainToken
        );
        assertNotNull( folderContent[ 0 ] );
    }

    @Test
    public void testShareContentGetting()
    {
        assertNotNull( shareContent[ 0 ] );
    }

    @Test
    public void testSharesGetting()
    {
        assertNotNull( shares[ 0 ] );
    }
}