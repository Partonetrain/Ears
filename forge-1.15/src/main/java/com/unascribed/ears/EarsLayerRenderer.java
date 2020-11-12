package com.unascribed.ears;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.unascribed.ears.common.EarsCommon;
import com.unascribed.ears.common.EarsFeaturesHolder;
import com.unascribed.ears.common.EarsLog;
import com.unascribed.ears.common.EarsRenderDelegate;
import com.unascribed.ears.common.NotRandom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Matrix3f;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.model.ModelRenderer.ModelBox;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

public class EarsLayerRenderer extends LayerRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>> implements EarsRenderDelegate {
	
	public EarsLayerRenderer(IEntityRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>> context) {
		super(context);
		EarsLog.debug("Platform:Renderer", "Constructed");
	}

	private MatrixStack m;
	private IVertexBuilder vc;
	private int light;
	private int overlay;
	private int skipRendering;
	private int stackDepth = 0;
	
	@Override
	public void render(MatrixStack m, IRenderTypeBuffer vertexConsumers, int light, AbstractClientPlayerEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
		EarsLog.debug("Platform:Renderer", "render({}, {}, {}, {}, {}, {}, {}, {}, {})", m, vertexConsumers, light, entity, limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch);
		ResourceLocation skin = getEntityTexture(entity);
		Texture tex = Minecraft.getInstance().getTextureManager().getTexture(skin);
		EarsLog.debug("Platform:Renderer", "render(...): skin={}, tex={}", skin, tex);
		if (tex instanceof EarsFeaturesHolder && !entity.isInvisible()) {
			EarsLog.debug("Platform:Renderer", "render(...): Checks passed");
			this.m = m;
			this.vc = vertexConsumers.getBuffer(RenderType.getEntityCutout(skin));
			this.light = light;
			this.overlay = LivingRenderer.getPackedOverlay(entity, 0);
			this.skipRendering = 0;
			this.stackDepth = 0;
			EarsCommon.render(((EarsFeaturesHolder)tex).getEarsFeatures(), this, limbDistance);
			this.m = null;
			this.vc = null;
		}
	}

	@Override
	public void push() {
		stackDepth++;
		m.push();
		if (skipRendering > 0) skipRendering++;
	}

	@Override
	public void pop() {
		if (stackDepth <= 0) {
			new Exception("STACK UNDERFLOW").printStackTrace();
			return;
		}
		stackDepth--;
		m.pop();
		if (skipRendering > 0) skipRendering--;
	}

	@Override
	public void anchorTo(BodyPart part) {
		ModelRenderer model;
		switch (part) {
			case HEAD:
				model = getEntityModel().bipedHead;
				break;
			case LEFT_ARM:
				model = getEntityModel().bipedLeftArm;
				break;
			case LEFT_LEG:
				model = getEntityModel().bipedLeftLeg;
				break;
			case RIGHT_ARM:
				model = getEntityModel().bipedRightArm;
				break;
			case RIGHT_LEG:
				model = getEntityModel().bipedRightLeg;
				break;
			case TORSO:
				model = getEntityModel().bipedBody;
				break;
			default: return;
		}
		if (!model.showModel) {
			EarsLog.debug("Platform:Renderer:Delegate", "anchorTo(...): Part is not visible, skip rendering until pop");
			if (skipRendering == 0) {
				skipRendering = 1;
			}
			return;
		}
		model.translateRotate(m);
		ModelBox cuboid = model.getRandomCube(NotRandom.INSTANCE);
		m.scale(1/16f, 1/16f, 1/16f);
		m.translate(cuboid.posX1, cuboid.posY2, cuboid.posZ1);
	}

	@Override
	public void translate(float x, float y, float z) {
		if (skipRendering > 0) return;
		m.translate(x, y, z);
	}

	@Override
	public void rotate(float ang, float x, float y, float z) {
		if (skipRendering > 0) return;
		m.rotate(new Vector3f(x, y, z).rotationDegrees(ang));
	}

	@Override
	public void renderFront(int u, int v, int w, int h, TexRotation rot, TexFlip flip) {
		if (skipRendering > 0) return;
		Matrix4f mv = m.getLast().getMatrix();
		Matrix3f mn = m.getLast().getNormal();
		
		float[][] uv = EarsCommon.calculateUVs(u, v, w, h, rot, flip);
		
		vc.pos(mv, 0, h, 0).color(1f, 1f, 1f, 1f).tex(uv[0][0], uv[0][1]).overlay(overlay).lightmap(light).normal(mn, 0, 0, -1).endVertex();
		vc.pos(mv, w, h, 0).color(1f, 1f, 1f, 1f).tex(uv[1][0], uv[1][1]).overlay(overlay).lightmap(light).normal(mn, 0, 0, -1).endVertex();
		vc.pos(mv, w, 0, 0).color(1f, 1f, 1f, 1f).tex(uv[2][0], uv[2][1]).overlay(overlay).lightmap(light).normal(mn, 0, 0, -1).endVertex();
		vc.pos(mv, 0, 0, 0).color(1f, 1f, 1f, 1f).tex(uv[3][0], uv[3][1]).overlay(overlay).lightmap(light).normal(mn, 0, 0, -1).endVertex();
	}

	@Override
	public void renderBack(int u, int v, int w, int h, TexRotation rot, TexFlip flip) {
		if (skipRendering > 0) return;
		Matrix4f mv = m.getLast().getMatrix();
		Matrix3f mn = m.getLast().getNormal();
		
		float[][] uv = EarsCommon.calculateUVs(u, v, w, h, rot, flip.flipHorizontally());
		
		vc.pos(mv, 0, 0, 0).color(1f, 1f, 1f, 1f).tex(uv[3][0], uv[3][1]).overlay(overlay).lightmap(light).normal(mn, 0, 0, 1).endVertex();
		vc.pos(mv, w, 0, 0).color(1f, 1f, 1f, 1f).tex(uv[2][0], uv[2][1]).overlay(overlay).lightmap(light).normal(mn, 0, 0, 1).endVertex();
		vc.pos(mv, w, h, 0).color(1f, 1f, 1f, 1f).tex(uv[1][0], uv[1][1]).overlay(overlay).lightmap(light).normal(mn, 0, 0, 1).endVertex();
		vc.pos(mv, 0, h, 0).color(1f, 1f, 1f, 1f).tex(uv[0][0], uv[0][1]).overlay(overlay).lightmap(light).normal(mn, 0, 0, 1).endVertex();
	}

	@Override
	public void renderDebugDot(float r, float g, float b, float a) {
		if (skipRendering > 0) return;
		Matrix4f mv = m.getLast().getMatrix();
		
		GL11.glPointSize(8);
		GlStateManager.disableTexture();
		BufferBuilder bb = Tessellator.getInstance().getBuffer();
		bb.begin(GL11.GL_POINTS, DefaultVertexFormats.POSITION_COLOR);
		bb.pos(mv, 0, 0, 0).color(r, g, b, a).endVertex();
		Tessellator.getInstance().draw();
		GlStateManager.enableTexture();
	}
}