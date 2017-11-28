package mockit.external.asm;

import javax.annotation.*;

final class FieldReader extends AnnotatedReader
{
   FieldReader(@Nonnull AnnotatedReader br) { super(br); }

   /**
    * Reads a field and makes the given visitor visit it.
    *
    * @param context information about the class being parsed.
    * @param u       the start offset of the field in the class file.
    * @return the offset of the first byte following the field in the class.
    */
   @Nonnegative
   int readField(@Nonnull ClassVisitor cv, @Nonnull Context context, @Nonnegative int u) {
      // Reads the field declaration.
      char[] c = buf;
      int access = readUnsignedShort(u);
      String name = readUTF8(u + 2, c);
      String desc = readUTF8(u + 4, c);
      u += 6;

      // Reads the field attributes.
      String signature = null;
      int anns = 0;
      Object value = null;

      for (int attributeCount = readUnsignedShort(u); attributeCount > 0; attributeCount--) {
         String attrName = readUTF8(u + 2, c);

         if ("ConstantValue".equals(attrName)) {
            int item = readUnsignedShort(u + 8);
            value = item == 0 ? null : readConst(item, c);
         }
         else if ("Signature".equals(attrName)) {
            signature = readUTF8(u + 8, c);
         }
         else if ("Deprecated".equals(attrName)) {
            access = Access.asDeprecated(access);
         }
         else if ("Synthetic".equals(attrName)) {
            access = Access.asSynthetic(access);
         }
         else if ("RuntimeVisibleAnnotations".equals(attrName)) {
            anns = u + 8;
         }

         u += 6 + readInt(u + 4);
      }

      u += 2;

      @SuppressWarnings("ConstantConditions") FieldVisitor fv = cv.visitField(access, name, desc, signature, value);

      if (fv == null) {
         return u;
      }

      readAnnotations(fv, c, anns);
      fv.visitEnd();
      return u;
   }

   private void readAnnotations(@Nonnull FieldVisitor fv, @Nonnull char[] c, @Nonnegative int anns) {
      if (anns != 0) {
         for (int annotationCount = readUnsignedShort(anns), v = anns + 2; annotationCount > 0; annotationCount--) {
            String desc = readUTF8(v, c);
            @SuppressWarnings("ConstantConditions") AnnotationVisitor av = fv.visitAnnotation(desc);

            v = annotationReader.readAnnotationValues(v + 2, c, true, av);
         }
      }
   }
}
