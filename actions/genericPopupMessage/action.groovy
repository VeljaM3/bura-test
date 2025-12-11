import com.vaadin.shared.ui.ContentMode;

v.caption("")

v.addSection("tekst")

v.setWidth("800px")
def style = p.noStyle ? "" : "h3, bold"
v.tekst.labelCustom(p.displayMessage, [style: style, id:"lab1"])

v.lab1.setContentMode(ContentMode.HTML)

v.addActionBar("buttonBar")
v.buttonBar.setMargin(true, false, false, false)
v.buttonBar.right.primarybutton("recycle.button.confirm", {
    outParams["agrees"] = true
    v.close()
},[iconId: Icons.CHECK])

v.buttonBar.left.link("recycle.button.cancel", {
    outParams["agrees"] = false
    v.close() 
},[style:"red"])