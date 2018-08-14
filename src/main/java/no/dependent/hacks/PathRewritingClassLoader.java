package no.dependent.hacks;



import java.io.IOException;
import java.lang.Override;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public abstract class PathRewritingClassLoader  extends URLClassLoader {
    public PathRewritingClassLoader(){
        super(null);
    }
    /*
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
    }*/
}