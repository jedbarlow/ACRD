package ab.biol498.ACRD;

import java.util.jar.JarFile;

import com.clcbio.api.base.plugin.PluginValidator;

public class ACRDValidator implements PluginValidator {
    @Override
    public void setJarFile(JarFile jarFile) {
    }

    @Override
    public boolean validate() {
        return true;
    }
}
