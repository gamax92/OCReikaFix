package gamax92.ocreikafix;

import java.util.HashSet;
import java.util.Iterator;

import org.apache.logging.log4j.Level;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.minecraft.launchwrapper.IClassTransformer;

public class OCReikaFixTransformer implements IClassTransformer {
	public static HashSet<String> classes = new HashSet<String>();

	private byte[] transformPatchVisibility(String name, String transformedName, byte[] bytes) {
		OCReikaFixCore.logMessage(Level.INFO, "Patching class: " + transformedName);

		ClassReader cr = new ClassReader(bytes);
		ClassNode cn = new ClassNode(Opcodes.ASM4);
		cr.accept(cn, 0);

		boolean patched = false;

		for (MethodNode mn : cn.methods) {
			if (mn.name.equals("getOCNetworkVisibility") && mn.desc.equals("()Lli/cil/oc/api/network/Visibility;")) {
				OCReikaFixCore.logMessage(Level.INFO, "Patching method: " + mn.name);
				Iterator<AbstractInsnNode> iter = mn.instructions.iterator();
				while (iter.hasNext()) {
					AbstractInsnNode currentNode = iter.next();

					if (currentNode instanceof FieldInsnNode && currentNode.getOpcode() == Opcodes.GETSTATIC) {
						FieldInsnNode killnode = (FieldInsnNode) currentNode;
						if (killnode.desc.equals("Lli/cil/oc/api/network/Visibility;") && killnode.name.equals("Network")) {
							AbstractInsnNode replaceWith = new FieldInsnNode(killnode.getOpcode(), killnode.owner, "Neighbors", killnode.desc);
							mn.instructions.set(killnode, replaceWith);
							patched = true;
						}
					}
				}
			}
		}

		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		cn.accept(cw);
		if (patched) {
			OCReikaFixCore.logMessage(Level.INFO, "Method was successfully patched!");
			bytes = cw.toByteArray();
		} else
			OCReikaFixCore.logMessage(Level.WARN, "No instances of Visibility.Network found.");

		return bytes;
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
		if (bytes != null) {
			if (classes.contains(transformedName)) {
				return transformPatchVisibility(name, transformedName, bytes);
			}
		}
		return bytes;
	}
}