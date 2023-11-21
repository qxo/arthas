package com.taobao.arthas.core.command.view;

import java.util.Collection;

import com.taobao.arthas.core.command.model.OptionVO;
import com.taobao.arthas.core.command.model.OptionsModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.text.Decoration;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.Overflow;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

/**
 * @author gongdewei 2020/4/15
 */
public class OptionsView extends ResultView<OptionsModel> {
    @Override
    public void draw(CommandProcess process, OptionsModel result) {
        final int width = process.width();
        final int tableWidth = width < 60 ? 57 : width - 3;
        if (result.getOptions() != null) {
            process.write(RenderUtil.render(drawShowTable(result.getOptions()), tableWidth));
        } else if (result.getChangeResult() != null) {
            TableElement table = ViewRenderUtil.renderChangeResult(result.getChangeResult());
            process.write(RenderUtil.render(table, tableWidth));
        }
        process.echoTips("screen with:" + width +"\n");
    }

    private Element drawShowTable(Collection<OptionVO> options) {
        TableElement table = new TableElement(1, 2, 5, 2, 8, 10)
                .leftCellPadding(1).rightCellPadding(1);
        //table.setOverflow(Overflow.HIDDEN);
        table.row(true, label("LEVEL", 5).style(Decoration.bold.bold()),
                label("TYPE", 7).style(Decoration.bold.bold()),
                label("NAME", 12).style(Decoration.bold.bold()),
                label("VALUE", 6).style(Decoration.bold.bold()),
                label("SUMMARY", 1).style(Decoration.bold.bold()),
                label("DESCRIPTION", 1).style(Decoration.bold.bold()));

        for (final OptionVO optionVO : options) {
            table.row(false, Element.label("" + optionVO.getLevel(), 5),
                    label(optionVO.getType(), 7),
                    label(optionVO.getName(), 10),
                    label(optionVO.getValue(), 6),
                    label(optionVO.getSummary(), 1),
                    label(optionVO.getDescription(), 1));
        }
        return table;
    }

    public static LabelElement label(final String value, final int minWith) {
        return new LabelElement(value, minWith < 5 ? 1 : minWith);
    }


    public static LabelElement label(final String value, final int minWith, final boolean forceWidth) {
        return new LabelElement(value, minWith < 5 ? 1 : minWith, forceWidth);
    }

    public static LabelElement label(final String value) {
        return label(value, 0);
    } 
}
