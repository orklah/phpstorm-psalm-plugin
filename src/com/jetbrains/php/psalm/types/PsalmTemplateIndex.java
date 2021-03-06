package com.jetbrains.php.psalm.types;

import com.intellij.psi.PsiElement;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.lang.PhpLangUtil;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocParamTag;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.inspections.parameterCountMismatch.PhpFuncGetArgUsageProvider;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpMetaTypeMappingsTable;
import com.jetbrains.php.lang.psi.resolve.types.PhpParameterBasedTypeProvider;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeSignatureKey;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpConstantNameIndex;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

public class PsalmTemplateIndex extends FileBasedIndexExtension<String, PhpMetaTypeMappingsTable> {
  @NonNls public static final ID<String, PhpMetaTypeMappingsTable> KEY = ID.create("php.psalm.template.index");
  @Override
  public @NotNull ID<String, PhpMetaTypeMappingsTable> getName() {
    return KEY;
  }

  @Override
  public @NotNull DataIndexer<String, PhpMetaTypeMappingsTable, FileContent> getIndexer() {
    return inputData -> {
      PhpFile file = ObjectUtils.tryCast(inputData.getPsiFile(), PhpFile.class);
      if (file == null) {
        return Collections.emptyMap();
      }
      return file.getTopLevelDefs().values().stream()
        .flatMap(PhpFuncGetArgUsageProvider::getFunctions)
        .map(PsalmTemplateIndex::getMap)
        .reduce(new HashMap<>(), ContainerUtil::union);
    };
  }

  private static Map<String, PhpMetaTypeMappingsTable> getMap(Function f) {
    PhpDocComment comment = f.getDocComment();
    if (comment == null) {
      return Collections.emptyMap();
    }
    Collection<String> templates = PsalmExtendedTypeProvider.getTemplates(comment);
    if (templates.isEmpty()) {
      return Collections.emptyMap();
    }
    Collection<PhpDocTag> returnTags = getReturnTags(comment);
    if (returnTags.isEmpty()) return Collections.emptyMap();
    List<PhpDocParamTag> paramTags = getParamTags(comment);
    List<Parameter> parameters = Arrays.asList(f.getParameters());
    int[] parametersIndicesToReturn = returnTags.stream()
      .flatMap(returnTag -> getDocTypesWithText(returnTag, templates).stream())
      .map(PsiElement::getText).distinct()
      .flatMap(returnTemplate -> paramNamesWithSameReturnTemplate(paramTags, returnTemplate)).distinct()
      .mapToInt(name -> ContainerUtil.indexOf(parameters, p -> PhpLangUtil.equalsParameterNames(name, p.getName())))
      .filter(i -> i >= 0)
      .toArray();
    if (parametersIndicesToReturn.length == 0) {
      return Collections.emptyMap();
    }
    PhpMetaTypeMappingsTable table = new PhpMetaTypeMappingsTable();
    for (int parameterIndexToReturn : parametersIndicesToReturn) {
      table.put(PhpParameterBasedTypeProvider.TYPE_KEY, String.valueOf(parameterIndexToReturn));
    }
    return Map.of(PhpTypeSignatureKey.getSignature(f), table);
  }

  private static Collection<PhpDocTag> getReturnTags(PhpDocComment comment) {
    Collection<PhpDocTag> res = new ArrayList<>();
    ContainerUtil.addIfNotNull(res, comment.getReturnTag());
    res.addAll(Arrays.asList(comment.getTagElementsByName("@psalm-return")));
    return res;
  }

  @NotNull
  private static List<PhpDocParamTag> getParamTags(PhpDocComment comment) {
    List<PhpDocParamTag> res = new ArrayList<>(comment.getParamTags());
    for (PhpDocTag tag : comment.getTagElementsByName("@psalm-param")) {
      res.add(((PhpDocParamTag)tag));
    }
    return res;
  }

  @NotNull
  private static Stream<String> paramNamesWithSameReturnTemplate(List<PhpDocParamTag> paramTags, String returnTemplate) {
    return paramTags.stream()
      .filter(tag -> !getDocTypesWithText(tag, Collections.singleton(returnTemplate)).isEmpty())
      .map(PhpDocParamTag::getVarName).filter(Objects::nonNull);
  }

  private static Collection<PhpDocType> getDocTypesWithText(@Nullable PhpDocTag tag, Collection<String> texts) {
    if (tag == null) {
      return Collections.emptyList();
    }
    Collection<PhpDocType> res = new ArrayList<>();
    PhpPsiElement child = tag.getFirstPsiChild();
    while (child != null) {
      if (child instanceof PhpDocType) {
        if (texts.contains(child.getText())) {
          res.add((PhpDocType)child);
        }
      }
      child = child.getNextPsiSibling();
    }
    return res;
  }

  @Override
  public @NotNull KeyDescriptor<String> getKeyDescriptor() {
    return EnumeratorStringDescriptor.INSTANCE;
  }

  @Override
  public @NotNull DataExternalizer<PhpMetaTypeMappingsTable> getValueExternalizer() {
    return PhpMetaTypeMappingsTable.EXTERNALIZER;
  }

  @Override
  public int getVersion() {
    return 2;
  }

  @Override
  public FileBasedIndex.@NotNull InputFilter getInputFilter() {
    return PhpConstantNameIndex.PHP_INPUT_FILTER;
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }
}
