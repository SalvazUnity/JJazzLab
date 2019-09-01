ref1 http://bits.netbeans.org/dev/javadoc/org-openide-actions/org/openide/actions/doc-files/api.html
ref2 http://bits.netbeans.org/dev/javadoc/org-openide-actions/org/openide/actions/package-summary.html
ref3 http://wiki.netbeans.org/FaqEditorMacros
ref4 http://wiki.netbeans.org/DevFaqActionContextSensitive 
ref5 http://bits.netbeans.org/dev/javadoc/org-openide-awt/org/openide/awt/Actions.html
ref6 http://bits.netbeans.org/dev/javadoc/org-openide-awt/org/openide/awt/ActionReference.html
ref7 http://bits.netbeans.org/dev/javadoc/org-openide-awt/org/openide/awt/ActionRegistration.html
ref8 http://bits.netbeans.org/dev/javadoc/org-openide-awt/org/openide/awt/ActionID.html
ref9 https://benkiew.wordpress.com/2012/12/28/netbeans-how-to-create-a-context-aware-action-with-an-icon-for-the-context-menu/

Register an action using @RegisterAnnotation allows to get the action created by Netbeans. Why doing this ?
- to get the default "lazy" instanciation (see ref 7 lazy option), in order to speed up app startup
- to easily code context sensitive actions that use "simple" enablement logic (see ref4 and ref5 context() method )
- if action is to be used in various places, toolbars, buttons, menus. Easy to do with @ActionReference
- to simplify global Shortcut assignation with @ActionReference (only if shortcut valid for the whole app). And allow shortcut customization by user ??

The action created using registration can be accessed programmatically with Actions.forID(...). 

LAZY INSTANCE: final action object is actually created only when actionPerformed() is done. In the meantime, the "virtual" initial action 
created by Netbeans has just the attributes set through the @RegisterAnnotation optional elements (displayname, icon, see ref 7), so it can be used 
to initialize a JButton even if action is not really instanced yet (but not all attributes are available, ACCELERATOR_KEY for example).
Lazy instanciation allow to accelerate app startup.
For actions of type ContextAwareAction or PopupPresenter or used in Popupmenu..., lazy should be set to false see ref7. For example to make sure 
that action attributes that can only be set when action is really instancied, such as putValue(ACCELERATOR_KEY), are taken into account 
when action is used to popuplate a popupmenu. 

CALLBACK ACTIONS
There are global actions with a standard shortcut, present in global menus or toolbars/buttons, such as DELETE, but which need different 
behaviors depending on focused TopComponent. See ref1 CallBack actions for clear explanations.
We can directly reuse a few of Netbeans predefined callback actions see ref2, but only the CallbackSystemAction instances (mainly cut/copy/paste/delete, and
note that DeleteAction is asynchronous).
To add other global callback actions (for example Edit since we cannot reuse the standard Netbeans EditAction which is not a CallbackSystemAction)
use @registration with key="xx", see ref5 callback() method. Usually I don't have fallback implementation so I create an empty class Edit.java 
with an empty body and just the annotation of  public static final String ACTION_MAP_KEY = "KeyInActionMap".
The delegate actions (found using ActionMap of the focusedComponent) may also be created using @registration for the reasons above.


ACTIONS enablement depend on only 1 type of selected object 
For example TransposerDown or SetEnd Bar
Simple: register the action so it is instanciated, the action must have a constructor
with objecttype parameter for action enabled if only 1 object selected
or List<objecttype> for action enabled if a several objects selected.

ACTIONS enablement is more complex, i.e. depend on several types of selected objects, 
OR need to be put in a context menu WITH shotcut displayed (see ref9)
For example DeleteItem
I created a base abstract action CL_EditorContextAction using ref4. 

 
SHORTCUTS: shortcuts keys defined in layer.xml for an action are global for an application ! 
If a same shortcut key has always same meaning but different operatinos depending on focused topcomponent (e.g DELETE),
then use callbacks actions, shortcut key can be defined in xml.
If a same shortcut key is used completly differently on 2 topcomponents (e.g. shift UP), then don't define shortcut in xml. Juste define
classic actions and bind it to a key in the topcomponent (usually in the controller).


Use NbPlatformAdapter modeul layer.xml to hide unuse items/actions.

