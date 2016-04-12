package gamax92.ocreikafix;

import java.util.HashSet;
import java.util.Iterator;

import org.apache.logging.log4j.Level;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

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

	private byte[] transformPatchNBT(String name, String transformedName, byte[] bytes) {
		OCReikaFixCore.logMessage(Level.INFO, "Patching class: " + transformedName);

		ClassReader cr = new ClassReader(bytes);
		ClassNode cn = new ClassNode(Opcodes.ASM4);
		cr.accept(cn, 0);

		boolean patched = false;

		for (MethodNode mn : cn.methods) {
			if ((mn.name.equals("readFromNBT") || mn.name.equals("func_145839_a")) && mn.desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;)V")) {
				OCReikaFixCore.logMessage(Level.INFO, "Patching method: " + mn.name);
				boolean clean = mn.name.equals("readFromNBT");

				Iterator<AbstractInsnNode> iter = mn.instructions.iterator();
				while (iter.hasNext()) {
					AbstractInsnNode currentNode = iter.next();

					if (currentNode instanceof TypeInsnNode && currentNode.getOpcode() == Opcodes.INSTANCEOF) {
						System.out.println("Found patching location!");

						AbstractInsnNode test = iter.next();

						if (test instanceof JumpInsnNode && test.getOpcode() == Opcodes.IFEQ) {
							InsnList toInject = new InsnList();

							toInject.add(new LabelNode(null));
							toInject.add(new VarInsnNode(Opcodes.ALOAD, 1));
							toInject.add(new LdcInsnNode("visibility"));
							toInject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", clean ? "hasKey" : "func_74764_b", "(Ljava/lang/String;)Z"));
							LabelNode LacksKey = new LabelNode(null);
							toInject.add(new JumpInsnNode(Opcodes.IFEQ, LacksKey));
							toInject.add(new LabelNode(null));
							toInject.add(new VarInsnNode(Opcodes.ALOAD, 1));
							toInject.add(new LdcInsnNode("visibility"));
							toInject.add(new VarInsnNode(Opcodes.ALOAD, 1));
							toInject.add(new LdcInsnNode("visibility"));
							toInject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", clean ? "getInteger" : "func_74762_e", "(Ljava/lang/String;)I"));
							toInject.add(new InsnNode(Opcodes.ICONST_1));
							toInject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Math", "min", "(II)I"));
							toInject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/nbt/NBTTagCompound", clean ? "setInteger" : "func_74768_a", "(Ljava/lang/String;I)V"));
							toInject.add(LacksKey);

							mn.instructions.insert(test, toInject);

							OCReikaFixCore.logMessage(Level.INFO, mn.name + " has been patched!");
							patched = true;

							break;
						} else
							OCReikaFixCore.logMessage(Level.ERROR, "Instruction check failed!");
					}
				}
			}
		}

		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		cn.accept(cw);
		if (patched) {
			OCReikaFixCore.logMessage(Level.INFO, "Method was successfully patched!");
			bytes = cw.toByteArray();
		} else {
			OCReikaFixCore.logMessage(Level.ERROR, "Failed to patch TileEntityBase.readFromNBT");
			OCReikaFixCore.logMessage(Level.ERROR, "Visibility will not update unless blocks are broken and put back down.");
		}

		return bytes;
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
		if (bytes != null) {
			if (transformedName.equals("Reika.DragonAPI.Base.TileEntityBase"))
				bytes = transformPatchNBT(name, transformedName, bytes);
			if (classes.contains(transformedName))
				return transformPatchVisibility(name, transformedName, bytes);
		}
		return bytes;
	}
}