package diobede.bbsaaa.particles;

import mchorse.bbs_mod.resources.Link;
import net.minecraft.util.Identifier;

/** One conversion shared by the picker and playback; saved links stay unchanged. */
public final class ParticleEffectIds
{
    private ParticleEffectIds()
    {}

    public static Identifier fromLink(Link link)
    {
        if (link == null || link.path == null || link.source == null)
        {
            return null;
        }

        String path = link.path.replace('\\', '/');
        if (path.startsWith("effeks/"))
        {
            path = path.substring(7);
        }
        if (path.endsWith(".efkefc"))
        {
            path = path.substring(0, path.length() - 7);
        }

        return path.isEmpty() ? null : Identifier.tryParse(link.source + ":" + path);
    }

    public static Link toLink(Identifier id)
    {
        return id == null ? null : new Link(id.getNamespace(), "effeks/" + id.getPath() + ".efkefc");
    }
}
