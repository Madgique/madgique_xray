package com.madgique.xray.gui.manage;

import com.madgique.xray.XRay;
import com.madgique.xray.gui.GuiSelectionScreen;
import com.madgique.xray.gui.utils.GuiBase;
import com.madgique.xray.gui.utils.GuiSlider;
import com.madgique.xray.reference.block.BlockData;
import com.madgique.xray.utils.OutlineColor;
import com.madgique.xray.utils.Utils;
import com.madgique.xray.xray.Controller;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;

import java.io.IOException;

public class GuiEdit extends GuiBase
{
    private GuiTextField oreName;
    private GuiTextField hexInput;
    private GuiSlider redSlider;
    private GuiSlider greenSlider;
    private GuiSlider blueSlider;
    private BlockData block;
    private int storeKey;

    private static final int BUTTON_DELETE = 100;
    private static final int BUTTON_SAVE = 98;
    private static final int BUTTON_CANCEL = 99;

    public GuiEdit(int storeKey, BlockData block) {
        super(true); // Has a sidebar
        this.setSideTitle( I18n.format("xray.single.tools") );

        this.storeKey = storeKey;
        this.block = block;
    }

    @Override
    public void initGui()
    {
        // Called when the gui should be (re)created
        // Sidebar buttons for now
        this.buttonList.add( new GuiButton( BUTTON_DELETE, (width / 2) + 78, height / 2 - 60, 120, 20, I18n.format("xray.single.delete") ));
        this.buttonList.add( new GuiButton( BUTTON_CANCEL, (width / 2) + 78, height / 2 + 58, 120, 20, I18n.format("xray.single.cancel") ));

        // Bottom buttons
        this.buttonList.add( new GuiButton( BUTTON_SAVE, width / 2 - 138, height / 2 + 83, 202, 20, I18n.format("xray.single.save") ) ); // Cancel button

        this.buttonList.add( redSlider = new GuiSlider( 3, width / 2 - 138, height / 2 + 7, I18n.format("xray.color.red"), 0, 255 ));
        this.buttonList.add( greenSlider = new GuiSlider( 2, width / 2 - 138, height / 2 + 30, I18n.format("xray.color.green"), 0, 255 ));
        this.buttonList.add( blueSlider = new GuiSlider( 1, width / 2 - 138, height / 2 + 53, I18n.format("xray.color.blue"), 0, 255 ) );

        redSlider.sliderValue   = (float)block.getColor().getRed()/255;
        greenSlider.sliderValue = (float)block.getColor().getGreen()/255;
        blueSlider.sliderValue  = (float)block.getColor().getBlue()/255;

        oreName = new GuiTextField( 1, this.fontRenderer, width / 2 - 138 ,  height / 2 - 63, 202, 20 );
        oreName.setText(this.block.getEntryName());

        hexInput = new GuiTextField( 2, this.fontRenderer, width / 2 + 10, height / 2 - 30, 54, 20 );
        hexInput.setMaxStringLength(7);
        hexInput.setText(currentHexString());
    }

    @Override
    public void actionPerformed( GuiButton button ) // Called on left click of GuiButton
    {
        switch(button.id)
        {
            case BUTTON_SAVE:
                BlockData block = new BlockData(
                        this.oreName.getText(),
                        this.block.getStateId(),
                        new OutlineColor((int)(redSlider.sliderValue * 255), (int)(greenSlider.sliderValue * 255), (int)(blueSlider.sliderValue * 255) ),
                        this.block.getItemStack(),
                        this.block.isDrawing(),
                        this.block.getOrder()
                );

                Controller.getBlockStore().getStore().put(this.storeKey, block);

                XRay.blockStore.write( Controller.getBlockStore().getStore() );

                mc.player.closeScreen();
                mc.displayGuiScreen( new GuiSelectionScreen() );
                break;

            case BUTTON_DELETE:
                // Write the store back to json on delete.
                Controller.getBlockStore().getStore().remove(this.storeKey);
                XRay.blockStore.write( Controller.getBlockStore().getStore() );

                mc.player.closeScreen();
                mc.displayGuiScreen( new GuiSelectionScreen() );
                break;

            case BUTTON_CANCEL:
                mc.player.closeScreen();
                mc.displayGuiScreen( new GuiSelectionScreen() );
                break;

            default:
                break;
        }
    }

    @Override
    protected void keyTyped( char par1, int par2 ) throws IOException // par1 is char typed, par2 is ascii hex (tab=15 return=28)
    {
        super.keyTyped( par1, par2 );

        if( hexInput.isFocused() ) {
            hexInput.textboxKeyTyped( par1, par2 );
            applyHexToSliders();
        }
        else if( oreName.isFocused() )
            oreName.textboxKeyTyped( par1, par2 );
    }

    @Override
    public void updateScreen()
    {
        oreName.updateCursorCounter();
        hexInput.updateCursorCounter();

        // Keep the text box in sync while the colour is changed with the sliders.
        if( !hexInput.isFocused() ) {
            String hex = currentHexString();
            if( !hex.equalsIgnoreCase(hexInput.getText()) )
                hexInput.setText(hex);
        }
    }

    @Override
    public void drawScreen( int x, int y, float f )
    {
        super.drawScreen(x, y, f);
        getFontRender().drawStringWithShadow(this.block.getItemStack().getDisplayName(), width / 2 - 138, height / 2 - 90, 0xffffff);

        oreName.drawTextBox();

        GuiAddBlock.renderPreview(width / 2 - 138, height / 2 - 40, 144, 45, redSlider.sliderValue, greenSlider.sliderValue, blueSlider.sliderValue);
        hexInput.drawTextBox();
        getFontRender().drawStringWithShadow(I18n.format("xray.color.hex"), width / 2 + 10, height / 2 - 40, 0xffffff);

        RenderHelper.enableGUIStandardItemLighting();
        this.itemRender.renderItemAndEffectIntoGUI( this.block.getItemStack(), width / 2 + 50, height / 2 - 105 );
        RenderHelper.disableStandardItemLighting();
    }

    @Override
    public void mouseClicked( int x, int y, int mouse ) throws IOException
    {
        super.mouseClicked( x, y, mouse );
        oreName.mouseClicked( x, y, mouse );

        if( isWithinHexInput(x, y) ) {
            // Select the whole value on click so typing or pasting replaces it
            hexInput.setFocused(true);
            hexInput.setCursorPosition(hexInput.getText().length());
            hexInput.setSelectionPos(0);
        }
        else
            hexInput.setFocused(false);
    }

    private boolean isWithinHexInput(int mouseX, int mouseY) {
        int hexX = width / 2 + 10, hexY = height / 2 - 30;
        return mouseX >= hexX && mouseX <= hexX + 54 && mouseY >= hexY && mouseY <= hexY + 20;
    }

    private String currentHexString() {
        return Utils.formatHexColor(
            (int)(redSlider.sliderValue * 255),
            (int)(greenSlider.sliderValue * 255),
            (int)(blueSlider.sliderValue * 255)
        );
    }

    private void applyHexToSliders() {
        int[] rgb = Utils.parseHexColor(hexInput.getText());
        if( rgb == null )
            return;

        redSlider.sliderValue   = rgb[0] / 255f;
        greenSlider.sliderValue = rgb[1] / 255f;
        blueSlider.sliderValue  = rgb[2] / 255f;
    }

    @Override
    public boolean hasTitle() {
        return true;
    }

    @Override
    public String title() {
        return I18n.format("xray.title.edit");
    }
}
