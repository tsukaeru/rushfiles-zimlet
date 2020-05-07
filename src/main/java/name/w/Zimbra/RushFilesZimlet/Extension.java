package name.w.Zimbra.RushFilesZimlet;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.extension.ExtensionDispatcherServlet;
import com.zimbra.cs.extension.ZimbraExtension;

public class Extension implements ZimbraExtension
{
    public String getName()
    {
        return "RushFilesZimlet";
    }

    public void init() throws ServiceException
    {
        ExtensionDispatcherServlet.register( this, new ExtensionHttpServlet() );
    }

    public void destroy()
    {
        ExtensionDispatcherServlet.unregister( this );
    }
}
