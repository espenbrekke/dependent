package no.dependent.hacks;



import java.io.IOException;
import java.lang.Override;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class PathRewritingClassLoader  extends URLClassLoader {
    /* The search path for classes and resources */
    private URLClassPath _ucp;

    /* The context to be used when loading classes and resources */
    private AccessControlContext _acc;

    private String prefix;

    public PathRewritingClassLoader(String prefix, URLClassLoader parent) {
        super(parent.getURLs(),parent);

        this.prefix=prefix+"/";

        _acc=AccessController.getContext();
        _ucp=new URLClassPath(getURLs(), _acc);
    }

    public PathRewritingClassLoader(String prefix,URL[] urls, ClassLoader parent) {
        super(urls,parent);

        this.prefix=prefix;

        try {
            Field fUcp = this.getClass().getDeclaredField("ucp"); //NoSuchFieldException
            fUcp.setAccessible(true);
            _ucp = (URLClassPath) fUcp.get(this);

            Field fAcc = this.getClass().getDeclaredField("ucp"); //NoSuchFieldException
            fAcc.setAccessible(true);
            _acc = (AccessControlContext) fAcc.get(this);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    /**
     * Finds and loads the class with the specified name from the URL search
     * path. Any URLs referring to JAR files are loaded and opened as needed
     * until the class is found.
     *
     * @param name the name of the class
     * @return the resulting class
     * @throws ClassNotFoundException if the class could not be found,
     *                                or if the loader is closed.
     * @throws NullPointerException   if {@code name} is {@code null}.
     */
    protected Class<?> findClass(final String name)
            throws ClassNotFoundException {
        final Class<?> result;
        try {
            result = AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Class<?>>() {
                        public Class<?> run() throws ClassNotFoundException {
                            String path = prefix+name.replace('.', '/').concat(".class");

                            Resource res = _ucp.getResource(path, false);
                            if (res != null) {
                                try {
                                    return _defineClass(name, res);
                                } catch (IOException e) {
                                    throw new ClassNotFoundException(name, e);
                                }
                            } else {
                                return null;
                            }
                        }
                    }, _acc);
        } catch (java.security.PrivilegedActionException pae) {
            throw (ClassNotFoundException) pae.getException();
        }
        if (result == null) {
            throw new ClassNotFoundException(name);
        }
        return result;
    }

    /*
     * Defines a Class using the class bytes obtained from the specified
     * Resource. The resulting Class must be resolved before it can be
     * used.
     */
    private Class<?> _defineClass(String name, Resource res) throws IOException {
        long t0 = System.nanoTime();
        int i = name.lastIndexOf('.');
        URL url = res.getCodeSourceURL();
        if (i != -1) {
            String pkgname = name.substring(0, i);
            // Check if package already loaded.
            Manifest man = res.getManifest();
            _definePackageInternal(pkgname, man, url);
        }
        // Now read the class bytes and define the class
        java.nio.ByteBuffer bb = res.getByteBuffer();
        if (bb != null) {
            // Use (direct) ByteBuffer:
            CodeSigner[] signers = res.getCodeSigners();
            CodeSource cs = new CodeSource(url, signers);
            PerfCounter.getReadClassBytesTime().addElapsedTimeFrom(t0);
            return defineClass(name, bb, cs);
        } else {
            byte[] b = res.getBytes();
            // must read certificates AFTER reading bytes.
            CodeSigner[] signers = res.getCodeSigners();
            CodeSource cs = new CodeSource(url, signers);
            PerfCounter.getReadClassBytesTime().addElapsedTimeFrom(t0);
            return defineClass(name, b, 0, b.length, cs);
        }
    }

    // Also called by VM to define Package for classes loaded from the CDS
    // archive
    private void _definePackageInternal(String pkgname, Manifest man, URL url)
    {
        if (_getAndVerifyPackage(pkgname, man, url) == null) {
            try {
                if (man != null) {
                    definePackage(pkgname, man, url);
                } else {
                    definePackage(pkgname, null, null, null, null, null, null, null);
                }
            } catch (IllegalArgumentException iae) {
                // parallel-capable class loaders: re-verify in case of a
                // race condition
                if (_getAndVerifyPackage(pkgname, man, url) == null) {
                    // Should never happen
                    throw new AssertionError("Cannot find package " +
                            pkgname);
                }
            }
        }
    }

    /*
     * Retrieve the package using the specified package name.
     * If non-null, verify the package using the specified code
     * source and manifest.
     */
    private Package _getAndVerifyPackage(String pkgname,
                                        Manifest man, URL url) {
        Package pkg = getPackage(pkgname);
        if (pkg != null) {
            // Package found, so check package sealing.
            if (pkg.isSealed()) {
                // Verify that code source URL is the same.
                if (!pkg.isSealed(url)) {
                    throw new SecurityException(
                            "sealing violation: package " + pkgname + " is sealed");
                }
            } else {
                // Make sure we are not attempting to seal the package
                // at this code source URL.
                if ((man != null) && _isSealed(pkgname, man)) {
                    throw new SecurityException(
                            "sealing violation: can't seal package " + pkgname +
                                    ": already loaded");
                }
            }
        }
        return pkg;
    }

    /*
 * Returns true if the specified package name is sealed according to the
 * given manifest.
 */
    private boolean _isSealed(String name, Manifest man) {
        String path = name.replace('.', '/').concat("/");
        Attributes attr = man.getAttributes(path);
        String sealed = null;
        if (attr != null) {
            sealed = attr.getValue(Attributes.Name.SEALED);
        }
        if (sealed == null) {
            if ((attr = man.getMainAttributes()) != null) {
                sealed = attr.getValue(Attributes.Name.SEALED);
            }
        }
        return "true".equalsIgnoreCase(sealed);
    }


    @Override
    public URL getResource(String name){
        return super.getResource(prefix+name);
    }
}