package java9;
import java.io.IOException;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class Rewriter {
  private static final Handle BSM = new Handle(Opcodes.H_INVOKESTATIC,
      Container.class.getName().replace('.', '/'),
      "bootstrap",
      MethodType.methodType(CallSite.class, Lookup.class, String.class, MethodType.class, MethodHandle.class).toMethodDescriptorString());
  
  static class InterceptedClassVisitor extends ClassVisitor {
    private static final String INTERCEPTED_NAME = 'L' + Interceptable.class.getName().replace('.', '/') + ';';
    boolean intercepted;

    InterceptedClassVisitor() {
      super(Opcodes.ASM5);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String annotationName, boolean visible) {
      if (annotationName.equals(INTERCEPTED_NAME)) {
        intercepted = true;
      }
      return null;
    }

    static boolean isIntercepted(ClassReader classReader) {
      InterceptedClassVisitor cv = new InterceptedClassVisitor();
      classReader.accept(cv, ClassReader.SKIP_CODE);
      return cv.intercepted;
    }
  }

  private static ClassReader openClassReader(Path path) {
    try {
      return new ClassReader(Files.newInputStream(path));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static int asMethodHandleTag(int opcode) {
    switch(opcode) {
    case Opcodes.GETFIELD:
      return Opcodes.H_GETFIELD;
    case Opcodes.PUTFIELD:
      return Opcodes.H_PUTFIELD;
    case Opcodes.INVOKESTATIC:
      return Opcodes.H_INVOKESTATIC;
    case Opcodes.INVOKEVIRTUAL:
      return Opcodes.H_INVOKEVIRTUAL;
    case Opcodes.INVOKEINTERFACE:
      return Opcodes.H_INVOKEINTERFACE;
    default:
      return 0;  // not a trapped opcode
    }
  }
  
  public static void main(String[] args) throws IOException {
    Path directory = Paths.get(args[0]);
    System.out.println("rewrite directory " + directory);
    List<Path> paths = Files.walk(directory)
        .filter(path -> path.getFileName().toString().endsWith(".class"))
        .collect(Collectors.toList());
    List<ClassReader> readers = paths.stream()
        .map(Rewriter::openClassReader)
        .collect(Collectors.toList());
    Set<String> annotatedTypes = readers.parallelStream()
        .filter(InterceptedClassVisitor::isIntercepted)
        .map(classReader -> classReader.getClassName().replace('.', '/'))
        .collect(Collectors.toSet());
    
    System.out.println("found annotated types " + annotatedTypes);
    
    for(int i = 0; i < readers.size(); i++) {
       int index = i;
       ClassReader classReader = readers.get(i);
       ClassWriter classWriter = new ClassWriter(classReader, 0);
       classReader.accept(new ClassVisitor(Opcodes.ASM5, classWriter) {
         boolean modified;
         
         @Override
         public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
           MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
           return new MethodVisitor(Opcodes.ASM5, mv) {
             private void rewriteAsInvokedynamic(int methodHandleTag, String owner, String name, String desc) {
               
               return;
             }
             
             @Override
             public void visitMethodInsn(int opcode, String owner, String name, String desc) {
               int methodHandleTag;
               if (annotatedTypes.contains(owner) && (methodHandleTag = asMethodHandleTag(opcode)) != 0) {
                 Handle handle = new Handle(methodHandleTag, owner, name, desc);
                 String indyDesc = '(' + ((owner.charAt(0) == '[')? owner: 'L' + owner +';') + desc.substring(1);
                 visitInvokeDynamicInsn(name, indyDesc, BSM, handle);
                 modified = true;
                 return;
               }
               super.visitMethodInsn(opcode, owner, name, desc);
             }
             
             @Override
            public void visitFieldInsn(int opcode, String owner, String name, String desc) {
               int methodHandleTag;
               if (annotatedTypes.contains(owner) && (methodHandleTag = asMethodHandleTag(opcode)) != 0) {
                 Handle handle = new Handle(methodHandleTag, owner, name, desc);
                 String indyDesc = '(' + ((owner.charAt(0) == '[')? owner: 'L' + owner +';') + ')' + desc;
                 visitInvokeDynamicInsn(name, indyDesc, BSM, handle);
                 modified = true;
                 return;
               }
               super.visitFieldInsn(opcode, owner, name, desc);
             }
           };
         }
         
         @Override
        public void visitEnd() {
           super.visitEnd();
           if (modified) {
             try {
               Path path = paths.get(index);
               Files.write(path, classWriter.toByteArray());
               System.out.println("rewrite " + path);
             } catch(IOException e) {
               throw new RuntimeException(e);
             }
           }
         }
       }, 0);
     }
  }
}
