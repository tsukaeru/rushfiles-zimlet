package name.w.RushFiles.Extension;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.extension.ExtensionDispatcherServlet;
import com.zimbra.cs.extension.ZimbraExtension;

public class RushFilesExtension implements ZimbraExtension
{
    public String getName()
    {
        return "MytestExtension";
    }

    public void init() throws ServiceException
    {
        ExtensionDispatcherServlet.register( this, new RushFiles() );
    }

    public void destroy()
    {
        ExtensionDispatcherServlet.unregister( this );
    }
}
