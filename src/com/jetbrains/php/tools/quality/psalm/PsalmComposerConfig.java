package com.jetbrains.php.tools.quality.psalm;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.php.composer.ComposerOpenSettingsProvider;
import com.jetbrains.php.composer.actions.log.ComposerLogMessageBuilder;
import com.jetbrains.php.tools.quality.QualityToolConfigurableList;
import com.jetbrains.php.tools.quality.QualityToolConfigurationManager;
import com.jetbrains.php.tools.quality.QualityToolType;
import com.jetbrains.php.tools.quality.QualityToolsComposerConfig;
import com.jetbrains.php.tools.quality.phpCSFixer.PhpCSFixerConfiguration;
import com.jetbrains.php.tools.quality.phpCSFixer.PhpCSFixerQualityToolType;
import com.jetbrains.php.ui.PhpUiUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class PsalmComposerConfig extends QualityToolsComposerConfig<PsalmConfiguration, PsalmValidationInspection> implements 
                                         ComposerOpenSettingsProvider {
  @NonNls private static final String PACKAGE = "vimeo/psalm";
  @NonNls private static final String RELATIVE_PATH = "bin/psalm";
  @NonNls private static final String PSALM_XML = "psalm.xml";
  private static final PsalmValidationInspection PSALM_VALIDATION_INSPECTION = new PsalmValidationInspection();


  public PsalmComposerConfig() {
    super(PACKAGE, RELATIVE_PATH);
  }

  @Override
  protected ComposerLogMessageBuilder.Settings getQualityToolsInspectionSettings() {
    return null;
  }

  @Override
  public PsalmValidationInspection getQualityInspection() {
    return PSALM_VALIDATION_INSPECTION;
  }

  @Override
  protected boolean applyRulesetFromComposer(@NotNull Project project, PsalmConfiguration configuration) {
    return false;
  }
  @Override
  protected boolean applyRulesetFromRoot(@NotNull Project project) {
    VirtualFile customRulesetFile = detectCustomRulesetFile(project.getBaseDir(), PSALM_XML);
    if(customRulesetFile == null){
      customRulesetFile = detectCustomRulesetFile(project.getBaseDir(), PSALM_XML + ".dist");
    }

    if (customRulesetFile != null) {
      final String path = customRulesetFile.getPath();
      return modifyRulesetInspectionSetting(project, tool -> applyRuleset(tool, path));
    }
    return false;
  }

  @Override
  public void openSettings(@NotNull Project project) {
    PhpUiUtil.editConfigurable(project, new QualityToolConfigurableList<PsalmConfiguration>(project, PsalmQualityToolType.INSTANCE, null) {
      @Override
      protected QualityToolType<PsalmConfiguration> getQualityToolType() {
        return PsalmQualityToolType.INSTANCE;
      }
    });
  }


  private static void applyRuleset(PsalmValidationInspection tool, String customRuleset) {
    tool.config = customRuleset;
  }

  @NotNull
  @Override
  protected QualityToolConfigurationManager<PsalmConfiguration> getConfigurationManager(@NotNull Project project) {
    return PsalmConfigurationManager.getInstance(project);
  }
}