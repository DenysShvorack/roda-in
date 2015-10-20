package rules;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Set;

import rules.filters.ContentFilter;
import utils.TreeVisitor;

/**
 * Created by adrapereira on 05-10-2015.
 */
public class SipPerFileVisitor extends Observable implements TreeVisitor {
    private static final int UPDATEFREQUENCY = 500; //in milliseconds
    private ArrayList<SipPreview> sips;
    private Set<ContentFilter> filters;
    private int added = 0, returned = 0;
    private long lastUIUpdate = 0;
    private String id;

    public SipPerFileVisitor(String id, Set<ContentFilter> filters){
        sips = new ArrayList<>();
        this.id = id;
        this.filters = filters;
    }

    public List<SipPreview> getSips() {
        return sips;
    }
    public int getCount(){
        return added;
    }
    public SipPreview getNext(){
        return sips.get(returned++);
    }
    public boolean hasNext(){
        return returned < added;
    }

    @Override
    public void preVisitDirectory(Path path, BasicFileAttributes attrs) {
    }

    @Override
    public void postVisitDirectory(Path path) {

    }

    private boolean filter(Path path){
        String pathString = path.toString();
        for(ContentFilter cf: filters){
            if(cf.filter(pathString))
                return true;
        }
        return false;
    }

    @Override
    public void visitFile(Path path, BasicFileAttributes attrs) {
        if(filter(path)) return;

        String name = "sip_" + path.getFileName().toString();
        TreeNode files = new TreeNode(path.toString());
        sips.add(new SipPreview(name, path.toString(), files));
        added ++;

        long now = System.currentTimeMillis();
        if(now - lastUIUpdate > UPDATEFREQUENCY) {
            setChanged();
            notifyObservers();
            lastUIUpdate = now;
        }
    }

    @Override
    public void visitFileFailed(Path path) {
    }

    @Override
    public void end() {
        setChanged();
        notifyObservers();
    }

    @Override
    public String getId(){
        return id;
    }
}
